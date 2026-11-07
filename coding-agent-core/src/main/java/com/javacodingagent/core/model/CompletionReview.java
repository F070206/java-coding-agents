package com.javacodingagent.core.model;
import java.util.List;
/** Observable requirement acceptance verdict, without hidden model reasoning. */
public record CompletionReview(Boolean completed, String summary, List<String> unmetRequirements) {
 public CompletionReview {
  if (completed == null || summary == null || summary.isBlank() || unmetRequirements == null
      || unmetRequirements.stream().anyMatch(value -> value == null || value.isBlank())
      || (completed && !unmetRequirements.isEmpty()) || (!completed && unmetRequirements.isEmpty()))
   throw new IllegalArgumentException("Invalid completion review");
  unmetRequirements = List.copyOf(unmetRequirements);
 }
}
