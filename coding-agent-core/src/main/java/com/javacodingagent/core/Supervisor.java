package com.javacodingagent.core;
import com.javacodingagent.common.domain.*;
import com.javacodingagent.core.llm.CodingModel;
import com.javacodingagent.core.model.TaskPlan;
import com.javacodingagent.core.repair.*;
import com.javacodingagent.core.state.AgentStateMachine;
import java.util.List;

/** Coordinates state transitions; file changes remain delegated to the tools module. */
public class Supervisor {
 private final CodingModel model; private final AgentStateMachine stateMachine = new AgentStateMachine();
 public Supervisor(CodingModel model) { this.model = model; }
 public TaskPlan createPlan(AgentExecutionContext context, String requirement, List<String> retrieved) { transition(context, AgentTaskStatus.PLANNING); return model.plan(requirement, retrieved); }
 public StructuredOutcome decideRepair(AgentExecutionContext context, SelfRepairPolicy policy, String error, String change, boolean unsafe, boolean contextExceeded, boolean confirmation) {
  transition(context, AgentTaskStatus.REPAIRING); RepairDecision decision = policy.evaluate(context.getCurrentRepairRound(), error, change, unsafe, contextExceeded, confirmation);
  if (!decision.continueRepair()) context.setTaskStatus(decision.stopReason() == AgentStopReason.HUMAN_CONFIRMATION_REQUIRED ? AgentTaskStatus.WAITING_CONFIRMATION : AgentTaskStatus.FAILED);
  return new StructuredOutcome(context.getTaskStatus(), decision.stopReason(), decision.explanation());
 }
 public void transition(AgentExecutionContext context, AgentTaskStatus to) { stateMachine.requireTransition(context.getTaskStatus(), to); context.setTaskStatus(to); }
 public record StructuredOutcome(AgentTaskStatus status, AgentStopReason stopReason, String explanation) { }
}
