package com.javacodingagent.core.llm;
import com.javacodingagent.core.model.*;
import java.util.List;
public class DeterministicCodingModel implements CodingModel {
 public TaskPlan plan(String requirement, List<String> context) { return new TaskPlan(requirement, List.of(), List.of("Controller", "Service", "Mapper", "Test"), List.of(), List.of(), "Run mvn test after a focused module test", List.of("Authentication and migration changes require confirmation")); }
 public ChangeProposal proposeChanges(TaskPlan plan, List<String> context, StructuredError previousError) { return new ChangeProposal("Mock model performs analysis without modifying files", List.of(), false); }
 public StructuredError analyzeError(String output) { return new StructuredError(output.contains("COMPILATION") ? "COMPILATION" : "TEST", null, null, null, "Command output requires source inspection", "Retrieve related symbols before patching", true); }
}
