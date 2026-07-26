package com.javacodingagent.core.model;
import java.util.List;
/** Structured planner output; it is persisted and traced without parsing prose. */
public record TaskPlan(String goal, List<String> targetModules, List<String> symbolsToRetrieve,
                       List<String> filesToRead, List<String> expectedChanges, String testStrategy,
                       List<String> risks) { }
