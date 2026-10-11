package com.javacodingagent.core;

import com.javacodingagent.core.llm.CodingModel;
import com.javacodingagent.core.model.*;
import com.javacodingagent.tools.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;

/** A bounded observe-act loop. Every round starts with fresh workspace observations. */
final class WorkspaceSession {
 enum Outcome { COMPLETED, WAITING_CONFIRMATION, LIMIT_REACHED }
 private final CodingModel model;
 private final WorkspaceGuard guard;
 private final FileTools files;
 private final WorkspaceTools workspace;
 private final BiConsumer<String, String> trace;
 private final Set<Path> changedPaths = new HashSet<>();

 WorkspaceSession(CodingModel model, WorkspaceGuard guard, FileTools files, BiConsumer<String, String> trace) {
  this.model = model; this.guard = guard; this.files = files;
  this.workspace = new WorkspaceTools(guard); this.trace = trace;
 }

 Outcome run(TaskPlan plan, StructuredError error) {
  List<String> observations = new ArrayList<>(); Set<Path> readPaths = new HashSet<>();
  observe(observations, "LIST_FILES", ".", workspace.list(".", null));
  if (plan.filesToRead() != null) for (String path : plan.filesToRead().stream().limit(20).toList()) {
   ToolResult<String> result = files.read(path, null, null);
   observe(observations, "READ_FILE", path, result);
   if (result.success()) readPaths.add(guard.resolve(path));
  }
  Set<Path> failedEdits = new HashSet<>();
  for (int step = 0; step < 40; step++) {
   while (observations.size() > 2 && observations.stream().mapToInt(String::length).sum() > 100_000) observations.remove(1);
   WorkspaceAction action = model.nextAction(plan, List.copyOf(observations), error);
   if (action == null || action.type() == null) throw new IllegalStateException("MODEL_DECISION_FAILED");
   switch (action.type()) {
    case LIST_FILES -> observe(observations, "LIST_FILES", action.path(), workspace.list(action.path(), action.query()));
    case SEARCH_FILES -> observe(observations, "SEARCH_FILES", action.path() + " query=" + action.query(), workspace.search(action.path(), action.query()));
    case READ_FILE -> {
     var result = files.read(action.path(), action.startLine(), action.endLine());
     observe(observations, "READ_FILE", action.path(), result);
     if (result.success()) readPaths.add(guard.resolve(action.path()));
    }
    case APPLY_CHANGES -> {
     ChangeProposal proposal = action.proposal();
     if (proposal == null || proposal.edits() == null) throw new IllegalStateException("MODEL_DECISION_FAILED");
     if (proposal.requiresConfirmation() || risky(proposal)) return Outcome.WAITING_CONFIRMATION;
     boolean success = true; Set<Path> editedInBatch = new HashSet<>();
     for (FileEdit edit : proposal.edits()) {
      ToolResult<String> result;
      Path requested = Path.of(edit.filePath()).normalize();
      try {
       Path target = guard.resolve(edit.filePath());
       if (edit.operation() == null) throw new IllegalArgumentException("Missing edit operation");
       if (edit.operation() == FileEdit.Operation.PATCH && !readPaths.contains(target))
        throw new IllegalArgumentException("Read the current file before patching it");
       result = edit.operation() == FileEdit.Operation.CREATE ? files.create(edit.filePath(), edit.content())
         : files.patch(edit.filePath(), edit.expected(), edit.replacement());
       if (result.success()) { changedPaths.add(target); editedInBatch.add(target); }
      } catch (Exception e) { result = ToolResult.failure("FileEditTool", "EDIT_REJECTED", e.getMessage(), System.currentTimeMillis()); }
      observe(observations, "FILE_EDIT", edit.filePath(), result);
      if (!result.success()) { failedEdits.add(requested); success = false; break; }
      failedEdits.remove(requested);
     }
     readPaths.removeAll(editedInBatch);
     if (success && failedEdits.isEmpty()) observations.add("APPLY_CHANGES OK: " + proposal.explanation());
     else observations.add("APPLY_CHANGES FAILED: inspect current files and correct unresolved edits before FINISH: " + failedEdits);
    }
    case FINISH -> {
     if (failedEdits.isEmpty()) return Outcome.COMPLETED;
     observations.add("FINISH rejected: a file edit is still unresolved. Read the file and apply a corrected change.");
    }
   }
  }
  return Outcome.LIMIT_REACHED;
 }

 private boolean risky(ChangeProposal proposal) {
  if (proposal.edits().size() > 10) return true;
  Set<Path> proposed = new HashSet<>(changedPaths);
  for (FileEdit edit : proposal.edits()) {
   if (edit == null || edit.filePath() == null) throw new IllegalStateException("MODEL_DECISION_FAILED");
   String path = edit.filePath().toLowerCase(Locale.ROOT);
   if (path.endsWith("pom.xml") || path.contains("migration") || path.contains("security") || path.contains("auth")) return true;
   try { proposed.add(guard.resolve(edit.filePath())); }
   catch (SecurityException e) { /* The file tool reports invalid paths as failed observations. */ }
  }
  return proposed.size() > 10;
 }

 private void observe(List<String> observations, String action, String path, ToolResult<String> result) {
  String text = action + " " + path + " " + (result.success() ? "OK: " + result.summary() + "\n" + result.data()
    : "FAILED: " + result.errorCode() + " " + result.errorMessage());
  observations.add(text.length() > 26_000 ? text.substring(0, 26_000) + "\n[truncated: narrow the path/query]" : text);
  String event = action.equals("FILE_EDIT") ? (result.success() ? "FILE_CHANGED" : "TOOL_FAILED") : "WORKSPACE_TOOL";
  trace.accept(event, action + " " + path + ": " + (result.success() ? result.summary() : result.errorCode()));
 }
}
