package com.javacodingagent.api.task;
import com.javacodingagent.common.domain.AgentTaskStatus;
public record TaskView(Long taskId, String traceId, Long repositoryId, String requirement, AgentTaskStatus status, int maxRepairCount, String stopReason, String message) {
 public TaskView(Long taskId, String traceId, Long repositoryId, String requirement, AgentTaskStatus status, int maxRepairCount) { this(taskId, traceId, repositoryId, requirement, status, maxRepairCount, null, null); }
}
