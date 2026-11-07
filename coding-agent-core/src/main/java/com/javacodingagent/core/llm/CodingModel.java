package com.javacodingagent.core.llm;

import com.javacodingagent.core.model.TaskPlan;
import com.javacodingagent.core.model.CompletionReview;
import com.javacodingagent.core.model.StructuredError;
import com.javacodingagent.core.model.ChangeProposal;
import com.javacodingagent.core.model.WorkspaceAction;
import java.util.List;

/** Adapter boundary for LangChain4j AI Services; tests use a deterministic implementation. */
public interface CodingModel {
 TaskPlan plan(String requirement, List<String> retrievedContext);
 ChangeProposal proposeChanges(TaskPlan plan, List<String> retrievedContext, StructuredError previousError);
 StructuredError analyzeError(String commandOutput);
 /** Merge independently generated plans while preserving the original requirement. */
 default TaskPlan mergePlans(String requirement, List<TaskPlan> plans) {
  return plan(requirement, List.of("Merge these independent candidate plans into one executable plan. Resolve conflicts and preserve all acceptance requirements: " + plans));
 }
 /** Adapters without an acceptance reviewer must never silently approve completion. */
 default CompletionReview reviewCompletion(String requirement, TaskPlan plan, List<String> evidence) {
  return new CompletionReview(false, "Completion verification unavailable", List.of("A model must verify the original requirement against workspace evidence"));
 }
 /** Legacy adapters can still propose a single batch; online adapters select tools iteratively. */
 default WorkspaceAction nextAction(TaskPlan plan, List<String> observations, StructuredError previousError) {
  if (observations.stream().anyMatch(value -> value.startsWith("APPLY_CHANGES OK")))
   return new WorkspaceAction(WorkspaceAction.Type.FINISH, null, null, null, null, null);
  return new WorkspaceAction(WorkspaceAction.Type.APPLY_CHANGES, null, null, null, null,
    proposeChanges(plan, observations, previousError));
 }
}
