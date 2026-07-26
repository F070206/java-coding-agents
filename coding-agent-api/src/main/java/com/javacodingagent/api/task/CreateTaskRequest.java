package com.javacodingagent.api.task;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull;
public record CreateTaskRequest(@NotNull Long repositoryId, @NotBlank String requirement, Integer maxRepairCount) { }
