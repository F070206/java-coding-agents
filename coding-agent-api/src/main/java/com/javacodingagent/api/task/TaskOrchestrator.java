package com.javacodingagent.api.task;

import com.javacodingagent.api.repository.InMemoryRepositoryService;
import com.javacodingagent.api.trace.TaskEventBroker;
import com.javacodingagent.common.domain.*;
import com.javacodingagent.core.CodingAgentEngine;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/** Async application service with one writer per repository. */
public class TaskOrchestrator {
 private static final Logger LOG = Logger.getLogger(TaskOrchestrator.class.getName());
 private final InMemoryTaskService tasks;
 private final InMemoryRepositoryService repositories;
 private final CodingAgentEngine engine;
 private final TaskEventBroker events;
 private final Executor executor;
 private final Map<Long, Semaphore> locks = new ConcurrentHashMap<>();

 public TaskOrchestrator(InMemoryTaskService tasks, InMemoryRepositoryService repositories, CodingAgentEngine engine, TaskEventBroker events, Executor executor) { this.tasks = tasks; this.repositories = repositories; this.engine = engine; this.events = events; this.executor = executor; }

 public TaskView submit(CreateTaskRequest request) {
  var repository = repositories.find(request.repositoryId()).orElseThrow(() -> new IllegalArgumentException("Repository not found"));
  TaskView task = tasks.create(request);
  events.publish(task.taskId(), "TASK_STARTED", task);
  CompletableFuture.runAsync(() -> {
   Semaphore lock = locks.computeIfAbsent(repository.id(), ignored -> new Semaphore(1));
   boolean acquired = false;
   try {
    acquired = lock.tryAcquire(1, TimeUnit.SECONDS);
    if (!acquired) {
     tasks.update(task.taskId(), AgentTaskStatus.WAITING_CONFIRMATION, "REPOSITORY_BUSY", "仓库正在执行其他任务，请在该任务结束后重新提交。");
     events.publish(task.taskId(), "WAITING_CONFIRMATION", tasks.find(task.taskId()).orElseThrow());
     return;
    }
    var context = new AgentExecutionContext();
    context.setTaskId(task.taskId());
    context.setTraceId(task.traceId());
    context.setSessionId(UUID.randomUUID().toString());
    context.setRepositoryPath(repository.workspacePath());
    var result = engine.execute(context, task.requirement(), task.maxRepairCount(), status -> {
     // Publish final states with their outcome below so polling cannot stop before it is available.
     switch (status) {
      case SCANNING, INDEXING, PLANNING, RETRIEVING, EXECUTING, REVIEWING, TESTING, REPAIRING -> tasks.update(task.taskId(), status);
      default -> { }
     }
    });
    tasks.update(task.taskId(), result.status(), result.stopReason().name(), message(result.stopReason()));
    String event = result.status() == AgentTaskStatus.WAITING_CONFIRMATION ? "WAITING_CONFIRMATION" : result.status() == AgentTaskStatus.SUCCEEDED ? "TASK_SUCCEEDED" : "TASK_FAILED";
    events.publish(task.taskId(), event, result);
   } catch (Exception e) {
    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
    // Do not expose model request bodies or credentials from exception messages.
    LOG.warning("Task " + task.taskId() + " failed with " + e.getClass().getSimpleName());
    tasks.update(task.taskId(), AgentTaskStatus.FAILED, "EXECUTION_ERROR", "执行异常，任务已停止。请检查仓库路径、模型配置和本机 Git/Maven 环境。");
    events.publish(task.taskId(), "TASK_FAILED", tasks.find(task.taskId()).orElseThrow());
   } finally {
    if (acquired) lock.release();
   }
  }, executor);
  return task;
 }

 private String message(AgentStopReason reason) {
  return switch (reason) {
   case TEST_PASSED -> "任务已完成，仓库测试通过。";
   case MAX_REPAIR_ATTEMPTS_REACHED -> "测试未通过，已达到最大修复次数。";
   case SAME_ERROR_REPEATED -> "重复出现相同错误，任务已停止。";
   case NO_EFFECTIVE_CHANGE -> "未产生有效变更，任务已停止。";
   case UNSAFE_OPERATION_DETECTED -> "检测到不安全操作，任务已停止。";
   case CONTEXT_LIMIT_REACHED -> "已达到上下文限制，任务已停止。";
   case HUMAN_CONFIRMATION_REQUIRED -> "变更涉及敏感操作，需要人工确认。";
   case MODEL_DECISION_FAILED -> "模型未能生成有效决策，任务已停止。";
  };
 }
}
