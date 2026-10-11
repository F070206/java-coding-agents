package com.javacodingagent.core;

import com.javacodingagent.core.llm.DeterministicCodingModel;
import com.javacodingagent.core.model.*;
import com.javacodingagent.tools.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceSessionTest {
 @TempDir Path root;
 private final TaskPlan plan = new DeterministicCodingModel().plan("Fix text", List.of());
 private WorkspaceSession session(DeterministicCodingModel model) {
  var guard = new WorkspaceGuard(root);
  return new WorkspaceSession(model, guard, new FileTools(guard, root.resolve(".agent-snapshots/test")), (type, summary) -> { });
 }
 private WorkspaceAction action(WorkspaceAction.Type type, String path) { return new WorkspaceAction(type, path, null, null, null, null); }
 private WorkspaceAction patch(String path, String before, String after) {
  return apply(new FileEdit(FileEdit.Operation.PATCH, path, before, after, null));
 }
 private WorkspaceAction apply(FileEdit edit) {
  return new WorkspaceAction(WorkspaceAction.Type.APPLY_CHANGES, null, null, null, null, new ChangeProposal("edit", List.of(edit), false));
 }

 @Test void feedsToolErrorsBackAndRecoversFromAStalePatch() throws Exception {
  Files.writeString(root.resolve("note.txt"), "current");
  var step = new AtomicInteger();
  var model = new DeterministicCodingModel() {
   @Override public WorkspaceAction nextAction(TaskPlan p, List<String> observations, StructuredError error) {
    return switch (step.getAndIncrement()) {
     case 0 -> action(WorkspaceAction.Type.READ_FILE, "note.txt");
     case 1 -> patch("note.txt", "stale", "fixed");
     case 2 -> { assertTrue(String.join("\n", observations).contains("PATCH_TARGET_NOT_FOUND")); yield action(WorkspaceAction.Type.READ_FILE, "note.txt"); }
     case 3 -> patch("note.txt", "current", "fixed");
     default -> action(WorkspaceAction.Type.FINISH, null);
    };
   }
  };
  assertEquals(WorkspaceSession.Outcome.COMPLETED, session(model).run(plan, null));
  assertEquals("fixed", Files.readString(root.resolve("note.txt")));
  assertEquals("current", Files.readString(root.resolve(".agent-snapshots/test/note.txt.bak")));
 }

 @Test void refusesBlindPatchesAndCannotFinishAfterFailedEdits() throws Exception {
  Files.writeString(root.resolve("note.txt"), "current"); var step = new AtomicInteger();
  var model = new DeterministicCodingModel() {
   @Override public WorkspaceAction nextAction(TaskPlan p, List<String> observations, StructuredError error) {
    return step.getAndIncrement() == 0 ? patch("note.txt", "current", "changed") : action(WorkspaceAction.Type.FINISH, null);
   }
  };
  assertEquals(WorkspaceSession.Outcome.LIMIT_REACHED, session(model).run(plan, null));
  assertEquals(40, step.get()); assertEquals("current", Files.readString(root.resolve("note.txt")));
 }

 @Test void countsChangedFilesAcrossSeparateActions() throws Exception {
  var step = new AtomicInteger();
  var model = new DeterministicCodingModel() {
   @Override public WorkspaceAction nextAction(TaskPlan p, List<String> observations, StructuredError error) {
    return apply(new FileEdit(FileEdit.Operation.CREATE, "file" + step.getAndIncrement() + ".txt", null, null, "new"));
   }
  };
  assertEquals(WorkspaceSession.Outcome.WAITING_CONFIRMATION, session(model).run(plan, null));
  assertTrue(Files.exists(root.resolve("file9.txt"))); assertFalse(Files.exists(root.resolve("file10.txt")));
 }

 @Test void keepsSensitiveChangesBehindConfirmation() {
  var model = new DeterministicCodingModel() {
   @Override public WorkspaceAction nextAction(TaskPlan p, List<String> observations, StructuredError error) {
    return apply(new FileEdit(FileEdit.Operation.CREATE, "pom.xml", null, null, "new"));
   }
  };
  assertEquals(WorkspaceSession.Outcome.WAITING_CONFIRMATION, session(model).run(plan, null));
  assertFalse(Files.exists(root.resolve("pom.xml")));
 }

 @Test void boundsEndlessReadRequests() {
  var count = new AtomicInteger();
  var model = new DeterministicCodingModel() {
   @Override public WorkspaceAction nextAction(TaskPlan p, List<String> observations, StructuredError error) {
    count.incrementAndGet(); return action(WorkspaceAction.Type.READ_FILE, "missing.txt");
   }
  };
  assertEquals(WorkspaceSession.Outcome.LIMIT_REACHED, session(model).run(plan, null)); assertEquals(40, count.get());
 }

 @Test void appliesMultiplePatchesToTheSameReadFileInOneBatch() throws Exception {
  Files.writeString(root.resolve("note.txt"), "first second"); var step = new AtomicInteger();
  var model = new DeterministicCodingModel() {
   @Override public WorkspaceAction nextAction(TaskPlan p, List<String> observations, StructuredError error) {
    return switch (step.getAndIncrement()) {
     case 0 -> action(WorkspaceAction.Type.READ_FILE, "note.txt");
     case 1 -> new WorkspaceAction(WorkspaceAction.Type.APPLY_CHANGES, null, null, null, null,
       new ChangeProposal("two patches", List.of(new FileEdit(FileEdit.Operation.PATCH, "note.txt", "first", "one", null),
         new FileEdit(FileEdit.Operation.PATCH, "note.txt", "second", "two", null)), false));
     default -> action(WorkspaceAction.Type.FINISH, null);
    };
   }
  };
  assertEquals(WorkspaceSession.Outcome.COMPLETED, session(model).run(plan, null));
  assertEquals("one two", Files.readString(root.resolve("note.txt")));
 }

 @Test void unrelatedSuccessfulEditDoesNotHideAnEarlierFailure() throws Exception {
  Files.writeString(root.resolve("note.txt"), "current"); var step = new AtomicInteger();
  var model = new DeterministicCodingModel() {
   @Override public WorkspaceAction nextAction(TaskPlan p, List<String> observations, StructuredError error) {
    return switch (step.getAndIncrement()) {
     case 0 -> patch("note.txt", "current", "changed");
     case 1 -> apply(new FileEdit(FileEdit.Operation.CREATE, "other.txt", null, null, "unrelated"));
     default -> action(WorkspaceAction.Type.FINISH, null);
    };
   }
  };
  assertEquals(WorkspaceSession.Outcome.LIMIT_REACHED, session(model).run(plan, null));
  assertEquals("current", Files.readString(root.resolve("note.txt")));
 }
}
