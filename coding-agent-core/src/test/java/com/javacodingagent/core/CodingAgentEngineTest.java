package com.javacodingagent.core;

import com.javacodingagent.common.domain.*;
import com.javacodingagent.core.llm.DeterministicCodingModel;
import com.javacodingagent.core.model.*;
import com.javacodingagent.tools.*;
import com.javacodingagent.trace.InMemoryTraceStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CodingAgentEngineTest {
 @TempDir Path root;
 private AgentExecutionContext context() {
  var context = new AgentExecutionContext(); context.setRepositoryPath(root.toString());
  context.setTaskId(1L); context.setTraceId("test"); return context;
 }

 @Test void editsLiveFilesAndRereadsChangedContentOnRepairWithoutIndexing() throws Exception {
  Files.writeString(root.resolve("README.md"), "value=old\n");
  var step = new AtomicInteger(); var tests = new AtomicInteger(); var traces = new InMemoryTraceStore();
  var model = new DeterministicCodingModel() {
   @Override public TaskPlan plan(String requirement, List<String> observations) {
    assertTrue(observations.get(0).contains("README.md"));
    assertFalse(observations.get(0).contains("value=old"));
    return super.plan(requirement, observations);
   }
   @Override public WorkspaceAction nextAction(TaskPlan plan, List<String> observations, StructuredError error) {
    int stage = step.getAndIncrement() % 4;
    if (stage == 0) return new WorkspaceAction(WorkspaceAction.Type.SEARCH_FILES, ".", "value=", null, null, null);
    if (stage == 1) {
     assertTrue(String.join("\n", observations).contains(error == null ? "value=old" : "value=intermediate"));
     return new WorkspaceAction(WorkspaceAction.Type.READ_FILE, "README.md", null, null, null, null);
    }
    if (stage == 2) {
     String joined = String.join("\n", observations);
     assertTrue(joined.contains("READ_FILE README.md OK"));
     if (error != null) { assertFalse(joined.contains("value=old")); assertTrue(joined.contains("value=intermediate")); }
     var edit = new FileEdit(FileEdit.Operation.PATCH, "README.md", error == null ? "old" : "intermediate", error == null ? "intermediate" : "fixed", null);
     return new WorkspaceAction(WorkspaceAction.Type.APPLY_CHANGES, null, null, null, null, new ChangeProposal("fix", List.of(edit), false));
    }
    return new WorkspaceAction(WorkspaceAction.Type.FINISH, null, null, null, null, null);
   }
  };
  var engine = new CodingAgentEngine(model, traces, guard -> new CommandTools(guard, Duration.ofSeconds(1)) {
   @Override public ToolResult<Map<String, Object>> execute(String command) {
    int exit = command.equals("mvn test") && tests.getAndIncrement() == 0 ? 1 : 0;
    return ToolResult.success("CommandTool", "completed", Map.of("exitCode", exit, "output", command + " " + tests.get()), System.currentTimeMillis());
   }
  });
  var statuses = new ArrayList<AgentTaskStatus>();
  var result = engine.execute(context(), "fix", 1, statuses::add);
  assertEquals(AgentTaskStatus.SUCCEEDED, result.status()); assertEquals(2, tests.get());
  assertEquals("value=fixed\n", Files.readString(root.resolve("README.md")));
  assertEquals("value=old\n", Files.readString(root.resolve(".agent-snapshots/test/README.md.bak")));
  assertFalse(statuses.contains(AgentTaskStatus.INDEXING)); assertFalse(statuses.contains(AgentTaskStatus.RETRIEVING));
  assertTrue(statuses.contains(AgentTaskStatus.REPAIRING));
  assertTrue(traces.findByTask(1L).stream().anyMatch(event -> event.type().equals("WORKSPACE_TOOL")));
  assertFalse(traces.findByTask(1L).stream().anyMatch(event -> event.type().equals("INDEX_COMPLETED")));
 }

 @Test void doesNotTestWhenTheToolLoopCannotFinish() throws Exception {
  var model = new DeterministicCodingModel() {
   @Override public WorkspaceAction nextAction(TaskPlan plan, List<String> observations, StructuredError error) {
    return new WorkspaceAction(WorkspaceAction.Type.LIST_FILES, ".", null, null, null, null);
   }
  };
  var engine = new CodingAgentEngine(model, new InMemoryTraceStore(), guard -> new CommandTools(guard, Duration.ofSeconds(1)) {
   @Override public ToolResult<Map<String, Object>> execute(String command) { fail("Commands must not run after tool exhaustion"); return null; }
  });
  var result = engine.execute(context(), "inspect", 0);
  assertEquals(AgentTaskStatus.FAILED, result.status()); assertEquals(AgentStopReason.CONTEXT_LIMIT_REACHED, result.stopReason());
 }
}
