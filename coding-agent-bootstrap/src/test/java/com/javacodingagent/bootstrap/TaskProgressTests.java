package com.javacodingagent.bootstrap;

import com.javacodingagent.api.HealthController;
import com.javacodingagent.api.repository.*;
import com.javacodingagent.api.task.*;
import com.javacodingagent.api.trace.TaskEventBroker;
import com.javacodingagent.common.domain.*;
import com.javacodingagent.core.CodingAgentEngine;
import com.javacodingagent.core.llm.*;
import com.javacodingagent.core.model.*;
import com.javacodingagent.trace.InMemoryTraceStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TaskProgressTests {
 @TempDir Path workspace;

 @Test void exposesPhaseDuringExecutionAndPersistsStopReason() throws Exception {
  var tasks = new InMemoryTaskService();
  CodingModel model = new DeterministicCodingModel() {
   @Override public TaskPlan plan(String requirement, List<String> context) {
    assertThat(tasks.find(1).orElseThrow().status()).isEqualTo(AgentTaskStatus.PLANNING);
    return super.plan(requirement, context);
   }
   @Override public ChangeProposal proposeChanges(TaskPlan plan, List<String> context, StructuredError error) {
    assertThat(tasks.find(1).orElseThrow().status()).isEqualTo(AgentTaskStatus.EXECUTING);
    return new ChangeProposal("Sensitive change", List.of(), true);
   }
  };
  var orchestrator = orchestrator(tasks, model);
  orchestrator.submit(new CreateTaskRequest(1L, "Inspect repository", 1));
  TaskView result = tasks.find(1).orElseThrow();
  assertThat(result.status()).isEqualTo(AgentTaskStatus.WAITING_CONFIRMATION);
  assertThat(result.stopReason()).isEqualTo("HUMAN_CONFIRMATION_REQUIRED");
  assertThat(result.message()).isNotBlank();
 }

 @Test void failureIsQueryableWithoutLeakingExceptionContent() throws Exception {
  var tasks = new InMemoryTaskService();
  var model = new DeterministicCodingModel() {
   @Override public TaskPlan plan(String requirement, List<String> context) { throw new IllegalStateException("private-request-content"); }
  };
  orchestrator(tasks, model).submit(new CreateTaskRequest(1L, "Inspect repository", 1));
  TaskView result = tasks.find(1).orElseThrow();
  assertThat(result.status()).isEqualTo(AgentTaskStatus.FAILED);
  assertThat(result.stopReason()).isEqualTo("EXECUTION_ERROR");
  assertThat(result.message()).isNotBlank().doesNotContain("private-request-content");
 }

 @Test void modeReflectsTheSelectedAdapterWithoutCallingIt() {
  assertThat(new HealthController(new DeterministicCodingModel()).health().data().get("mode")).isEqualTo("OFFLINE");
  assertThat(new HealthController(mock(CodingModel.class)).health().data().get("mode")).isEqualTo("ONLINE");
 }

 @ParameterizedTest
 @EnumSource(value = AgentTaskStatus.class, names = {"SUCCEEDED", "FAILED", "WAITING_CONFIRMATION"})
 void publishesFinalStatusTogetherWithOutcome(AgentTaskStatus finalStatus) {
  var tasks = new InMemoryTaskService();
  var repositories = new InMemoryRepositoryService();
  repositories.create(new CreateRepositoryRequest("demo", workspace.toString(), 1L));
  AgentStopReason reason = switch (finalStatus) {
   case SUCCEEDED -> AgentStopReason.TEST_PASSED;
   case WAITING_CONFIRMATION -> AgentStopReason.HUMAN_CONFIRMATION_REQUIRED;
   default -> AgentStopReason.MAX_REPAIR_ATTEMPTS_REACHED;
  };
  var observedBeforeResult = new AtomicReference<TaskView>();
  var engine = new CodingAgentEngine(null, new InMemoryTraceStore()) {
   @Override public Result execute(AgentExecutionContext context, String requirement, int maxRepairs, Consumer<AgentTaskStatus> onStatus) {
    onStatus.accept(AgentTaskStatus.EXECUTING);
    onStatus.accept(finalStatus);
    // A poll before execute returns must not observe a terminal state without its outcome.
    observedBeforeResult.set(tasks.find(context.getTaskId()).orElseThrow());
    return new Result(finalStatus, reason, null);
   }
  };
  new TaskOrchestrator(tasks, repositories, engine, new TaskEventBroker(), Runnable::run)
    .submit(new CreateTaskRequest(1L, "Inspect repository", 1));
  assertThat(observedBeforeResult.get().status()).isEqualTo(AgentTaskStatus.EXECUTING);
  TaskView result = tasks.find(1).orElseThrow();
  assertThat(result.status()).isEqualTo(finalStatus);
  assertThat(result.stopReason()).isEqualTo(reason.name());
  assertThat(result.message()).isNotBlank();
 }

 private TaskOrchestrator orchestrator(InMemoryTaskService tasks, CodingModel model) throws Exception {
  Files.writeString(workspace.resolve("pom.xml"), "<project><modelVersion>4.0.0</modelVersion><groupId>demo</groupId><artifactId>demo</artifactId><version>1</version></project>");
  var repositories = new InMemoryRepositoryService();
  repositories.create(new CreateRepositoryRequest("demo", workspace.toString(), 1L));
  var engine = new CodingAgentEngine(model, new InMemoryTraceStore());
  return new TaskOrchestrator(tasks, repositories, engine, new TaskEventBroker(), Runnable::run);
 }
}
