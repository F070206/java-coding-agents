package com.javacodingagent.api.task;
import com.javacodingagent.common.domain.AgentTaskStatus;
public record TaskView(Long taskId, String traceId, Long repositoryId, String requirement, AgentTaskStatus status, int maxRepairCount) { }
