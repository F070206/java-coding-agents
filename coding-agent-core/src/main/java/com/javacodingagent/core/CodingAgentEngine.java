package com.javacodingagent.core;

import com.javacodingagent.analysis.*; import com.javacodingagent.common.domain.*; import com.javacodingagent.core.llm.CodingModel; import com.javacodingagent.core.model.*; import com.javacodingagent.rag.*; import com.javacodingagent.tools.*; import com.javacodingagent.trace.*;
import java.nio.file.*; import java.time.*; import java.util.*; import java.util.concurrent.atomic.AtomicLong;
import com.javacodingagent.core.repair.SelfRepairPolicy;

/** Executes the auditable scan-index-plan-retrieve-edit-test-repair workflow for one repository. */
public class CodingAgentEngine {
 private final CodingModel model; private final TraceStore traces; private final AtomicLong sequence = new AtomicLong();
 public CodingAgentEngine(CodingModel model, TraceStore traces) { this.model = model; this.traces = traces; }
 public Result execute(AgentExecutionContext context, String requirement, int maxRepairs) throws Exception {
  Path root = Path.of(context.getRepositoryPath()); var guard = new WorkspaceGuard(root); var fileTools = new FileTools(guard, root.resolve(".agent-snapshots").resolve(context.getTraceId())); var commands = new CommandTools(guard, Duration.ofMinutes(5));
  context.setTaskStatus(AgentTaskStatus.SCANNING); var profile = new RepositoryScanner().scan(root); trace(context, "REPOSITORY_SCANNED", profile.toString());
  context.setTaskStatus(AgentTaskStatus.INDEXING); var store = new InMemoryCodeIndexStore(); int changed = new IncrementalCodeIndexer(new JavaSourceAnalyzer(), new CodeChunker(), store).index(String.valueOf(context.getTaskId()), "root", root); trace(context, "INDEX_COMPLETED", changed + " files indexed");
  context.setTaskStatus(AgentTaskStatus.PLANNING); List<String> initial = store.all(String.valueOf(context.getTaskId())).stream().limit(20).map(c -> c.filePath() + "\n" + c.content()).toList(); TaskPlan plan = model.plan(requirement, initial); trace(context, "PLAN_CREATED", plan.toString());
  context.setTaskStatus(AgentTaskStatus.RETRIEVING); var retriever = new HybridCodeRetriever(store); List<String> retrieved = plan.symbolsToRetrieve().stream().flatMap(q -> retriever.search(String.valueOf(context.getTaskId()), q, 5).stream()).distinct().map(r -> r.chunk().filePath() + "\n" + r.chunk().content()).limit(30).toList();
  StructuredError error = null; var policy = new SelfRepairPolicy(maxRepairs); for (int round = 0; round <= maxRepairs; round++) { context.setCurrentRepairRound(round); context.setTaskStatus(round == 0 ? AgentTaskStatus.EXECUTING : AgentTaskStatus.REPAIRING); ChangeProposal proposal = model.proposeChanges(plan, retrieved, error); if (proposal.requiresConfirmation() || risky(proposal)) { context.setTaskStatus(AgentTaskStatus.WAITING_CONFIRMATION); trace(context, "WAITING_CONFIRMATION", "Risk-sensitive change requires approval"); return new Result(context.getTaskStatus(), AgentStopReason.HUMAN_CONFIRMATION_REQUIRED, plan); }
   for (FileEdit edit : proposal.edits()) { ToolResult<String> result = edit.operation() == FileEdit.Operation.CREATE ? fileTools.create(edit.filePath(), edit.content()) : fileTools.patch(edit.filePath(), edit.expected(), edit.replacement()); trace(context, "FILE_CHANGED", result.summary()); if (!result.success()) throw new IllegalStateException(result.errorMessage()); }
   context.setTaskStatus(AgentTaskStatus.REVIEWING); var diff = commands.execute("git diff"); trace(context, "DIFF_REVIEWED", diff.summary()); context.setTaskStatus(AgentTaskStatus.TESTING); var test = commands.execute("mvn test"); int exit = test.data() == null ? -1 : (Integer)test.data().get("exitCode"); trace(context, exit == 0 ? "TEST_PASSED" : "TEST_FAILED", test.summary()); if (exit == 0) { context.setTaskStatus(AgentTaskStatus.SUCCEEDED); return new Result(context.getTaskStatus(), AgentStopReason.TEST_PASSED, plan); }
   error = model.analyzeError(String.valueOf(test.data())); var decision = policy.evaluate(round, Integer.toHexString(error.hashCode()), String.valueOf(diff.data()).hashCode() + "", false, false, false); if (!decision.continueRepair()) { context.setTaskStatus(AgentTaskStatus.FAILED); return new Result(context.getTaskStatus(), decision.stopReason(), plan); }
  } context.setTaskStatus(AgentTaskStatus.FAILED); return new Result(context.getTaskStatus(), AgentStopReason.MAX_REPAIR_ATTEMPTS_REACHED, plan);
 }
 private void trace(AgentExecutionContext c, String type, String summary) { traces.append(new TraceEvent(c.getTraceId(), c.getTaskId(), sequence.incrementAndGet(), type, "SUPERVISOR", summary, Map.of("status", c.getTaskStatus().name()), Instant.now())); }
 private boolean risky(ChangeProposal proposal) { if (proposal.edits().size() > 10) return true; return proposal.edits().stream().map(FileEdit::filePath).map(String::toLowerCase).anyMatch(path -> path.endsWith("pom.xml") || path.contains("migration") || path.contains("security") || path.contains("auth")); }
 public record Result(AgentTaskStatus status, AgentStopReason stopReason, TaskPlan plan) { }
}
