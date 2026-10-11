package com.javacodingagent.core;

import com.javacodingagent.common.domain.*;
import com.javacodingagent.core.llm.CodingModel;
import com.javacodingagent.core.model.*;
import com.javacodingagent.core.repair.SelfRepairPolicy;
import com.javacodingagent.core.state.AgentStateMachine;
import com.javacodingagent.tools.*;
import com.javacodingagent.trace.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/** Executes a live workspace plan-tool-test-repair workflow without a code index. */
public class CodingAgentEngine {
 private final CodingModel model;
 private final TraceStore traces;
 private final AtomicLong sequence = new AtomicLong();
 private final Function<WorkspaceGuard, CommandTools> commandFactory;
 public CodingAgentEngine(CodingModel model, TraceStore traces) {
  this(model, traces, guard -> new CommandTools(guard, Duration.ofMinutes(5)));
 }
 CodingAgentEngine(CodingModel model, TraceStore traces, Function<WorkspaceGuard, CommandTools> commandFactory) {
  this.model = model; this.traces = traces; this.commandFactory = commandFactory;
 }
 public Result execute(AgentExecutionContext context, String requirement, int maxRepairs) throws Exception {
  return execute(context, requirement, maxRepairs, status -> { });
 }
 public Result execute(AgentExecutionContext context, String requirement, int maxRepairs, Consumer<AgentTaskStatus> onStatus) throws Exception {
  var stateMachine = new AgentStateMachine();
  Consumer<AgentTaskStatus> transition = status -> {
   stateMachine.requireTransition(context.getTaskStatus(), status);
   context.setTaskStatus(status); onStatus.accept(status);
  };
  var guard = new WorkspaceGuard(Path.of(context.getRepositoryPath()));
  var fileTools = new FileTools(guard, guard.root().resolve(".agent-snapshots").resolve(context.getTraceId()));
  var commands = commandFactory.apply(guard);
  transition.accept(AgentTaskStatus.SCANNING);
  var inventory = new WorkspaceTools(guard).list(".", null);
  if (!inventory.success()) throw new IllegalStateException(inventory.errorCode());
  trace(context, "REPOSITORY_SCANNED", inventory.summary());
  transition.accept(AgentTaskStatus.PLANNING);
  TaskPlan plan = model.plan(requirement, List.of("Live workspace files (no code index): " + inventory.summary() + "\n" + inventory.data()));
  if (plan == null) throw new IllegalStateException("MODEL_DECISION_FAILED");
  trace(context, "PLAN_CREATED", plan.toString());
  var session = new WorkspaceSession(model, guard, fileTools, (type, summary) -> trace(context, type, summary));
  StructuredError error = null; var policy = new SelfRepairPolicy(maxRepairs);
  for (int round = 0; round <= maxRepairs; round++) {
   context.setCurrentRepairRound(round);
   transition.accept(round == 0 ? AgentTaskStatus.EXECUTING : AgentTaskStatus.REPAIRING);
   var outcome = session.run(plan, error);
   if (outcome == WorkspaceSession.Outcome.WAITING_CONFIRMATION) {
    transition.accept(AgentTaskStatus.WAITING_CONFIRMATION);
    trace(context, "WAITING_CONFIRMATION", "Risk-sensitive change requires approval");
    return new Result(context.getTaskStatus(), AgentStopReason.HUMAN_CONFIRMATION_REQUIRED, plan);
   }
   if (outcome == WorkspaceSession.Outcome.LIMIT_REACHED) {
    transition.accept(AgentTaskStatus.FAILED);
    return new Result(context.getTaskStatus(), AgentStopReason.CONTEXT_LIMIT_REACHED, plan);
   }
   transition.accept(AgentTaskStatus.REVIEWING);
   var diff = commands.execute("git diff"); trace(context, "DIFF_REVIEWED", diff.summary());
   transition.accept(AgentTaskStatus.TESTING);
   var test = commands.execute("mvn test");
   int exit = test.success() && test.data() != null ? (Integer)test.data().get("exitCode") : -1;
   trace(context, exit == 0 ? "TEST_PASSED" : "TEST_FAILED", test.summary());
   if (exit == 0) {
    transition.accept(AgentTaskStatus.SUCCEEDED);
    return new Result(context.getTaskStatus(), AgentStopReason.TEST_PASSED, plan);
   }
   error = model.analyzeError(test.success() ? String.valueOf(test.data()) : test.errorCode() + ": " + test.errorMessage());
   var decision = policy.evaluate(round, Integer.toHexString(error.hashCode()), String.valueOf(diff.data()).hashCode() + "", false, false, false);
   if (!decision.continueRepair()) {
    transition.accept(AgentTaskStatus.FAILED);
    return new Result(context.getTaskStatus(), decision.stopReason(), plan);
   }
  }
  transition.accept(AgentTaskStatus.FAILED);
  return new Result(context.getTaskStatus(), AgentStopReason.MAX_REPAIR_ATTEMPTS_REACHED, plan);
 }
 private void trace(AgentExecutionContext c, String type, String summary) {
  traces.append(new TraceEvent(c.getTraceId(), c.getTaskId(), sequence.incrementAndGet(), type, "SUPERVISOR", summary, Map.of("status", c.getTaskStatus().name()), Instant.now()));
 }
 public record Result(AgentTaskStatus status, AgentStopReason stopReason, TaskPlan plan) { }
}
