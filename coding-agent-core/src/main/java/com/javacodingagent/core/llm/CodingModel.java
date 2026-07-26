package com.javacodingagent.core.llm;

import com.javacodingagent.core.model.TaskPlan;
import com.javacodingagent.core.model.StructuredError;
import com.javacodingagent.core.model.ChangeProposal;
import java.util.List;

/** Adapter boundary for LangChain4j AI Services; tests use a deterministic implementation. */
public interface CodingModel {
 TaskPlan plan(String requirement, List<String> retrievedContext);
 ChangeProposal proposeChanges(TaskPlan plan, List<String> retrievedContext, StructuredError previousError);
 StructuredError analyzeError(String commandOutput);
}
