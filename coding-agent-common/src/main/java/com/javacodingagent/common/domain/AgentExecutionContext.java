package com.javacodingagent.common.domain;

import java.util.HashMap;
import java.util.Map;

/** Shared, task-scoped context. All modules must propagate this instance. */
public class AgentExecutionContext {
    private String traceId;
    private Long taskId;
    private String sessionId;
    private Long userId;
    private String repositoryPath;
    private AgentTaskStatus taskStatus = AgentTaskStatus.PENDING;
    private Integer currentRepairRound = 0;
    private final Map<String, Object> attributes = new HashMap<>();
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRepositoryPath() { return repositoryPath; }
    public void setRepositoryPath(String repositoryPath) { this.repositoryPath = repositoryPath; }
    public AgentTaskStatus getTaskStatus() { return taskStatus; }
    public void setTaskStatus(AgentTaskStatus taskStatus) { this.taskStatus = taskStatus; }
    public Integer getCurrentRepairRound() { return currentRepairRound; }
    public void setCurrentRepairRound(Integer currentRepairRound) { this.currentRepairRound = currentRepairRound; }
    public Map<String, Object> getAttributes() { return attributes; }
}
