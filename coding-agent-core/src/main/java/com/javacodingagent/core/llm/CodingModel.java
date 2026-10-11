package com.javacodingagent.core.llm;

import com.javacodingagent.core.model.TaskPlan;
import com.javacodingagent.core.model.StructuredError;
import com.javacodingagent.core.model.ChangeProposal;
import com.javacodingagent.core.model.WorkspaceAction;
import java.util.List;

/** Adapter boundary for LangChain4j AI Services; tests use a deterministic implementation. */
public interface CodingModel {
 TaskPlan plan(String requirement, List<String> retrievedContext);
 ChangeProposal proposeChanges(TaskPlan plan, List<String> retrievedContext, StructuredError previousError);
 StructuredError analyzeError(String commandOutput);
 /** Legacy adapters can still propose a single batch; online adapters select tools iteratively. */
 default WorkspaceAction nextAction(TaskPlan plan, List<String> observations, StructuredError previousError) {
  if (observations.stream().anyMatch(value -> value.startsWith("APPLY_CHANGES OK")))
   return new WorkspaceAction(WorkspaceAction.Type.FINISH, null, null, null, null, null);
  return new WorkspaceAction(WorkspaceAction.Type.APPLY_CHANGES, null, null, null, null,
    proposeChanges(plan, observations, previousError));
 }
}
