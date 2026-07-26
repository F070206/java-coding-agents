package com.javacodingagent.api.task;
import com.javacodingagent.common.domain.AgentTaskStatus;
import java.util.*; import java.util.concurrent.*; import java.util.concurrent.atomic.AtomicLong;
/** Development persistence adapter; infrastructure stage replaces this with MySQL/Redis/MQ adapters. */
public class InMemoryTaskService { private final AtomicLong ids = new AtomicLong(); private final Map<Long, TaskView> tasks = new ConcurrentHashMap<>();
 public TaskView create(CreateTaskRequest request) { long id = ids.incrementAndGet(); var task = new TaskView(id, UUID.randomUUID().toString(), request.repositoryId(), request.requirement(), AgentTaskStatus.PENDING, request.maxRepairCount() == null ? 3 : request.maxRepairCount()); tasks.put(id, task); return task; }
 public Optional<TaskView> find(long id) { return Optional.ofNullable(tasks.get(id)); }
 public Optional<TaskView> update(long id, AgentTaskStatus status) { return find(id).map(old -> { var next = new TaskView(old.taskId(), old.traceId(), old.repositoryId(), old.requirement(), status, old.maxRepairCount()); tasks.put(id, next); return next; }); }
}
