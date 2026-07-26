package com.javacodingagent.api.repository;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull;
public record CreateRepositoryRequest(@NotBlank String name, @NotBlank String workspacePath, @NotNull Long userId) { }
