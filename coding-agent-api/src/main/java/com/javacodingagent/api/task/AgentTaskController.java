package com.javacodingagent.api.task;
import com.javacodingagent.common.domain.AgentTaskStatus; import com.javacodingagent.common.web.ApiResponse;
import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.util.Map;
import com.javacodingagent.trace.TraceStore;
@RestController @RequestMapping("/api/agent/tasks")
public class AgentTaskController { private final InMemoryTaskService tasks; private final TaskOrchestrator orchestrator; private final TraceStore traces; public AgentTaskController(InMemoryTaskService tasks, TaskOrchestrator orchestrator, TraceStore traces) { this.tasks = tasks; this.orchestrator = orchestrator; this.traces = traces; }
 @PostMapping public ResponseEntity<ApiResponse<TaskView>> create(@Valid @RequestBody CreateTaskRequest request) { return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(orchestrator.submit(request))); }
 @GetMapping("/{taskId}") public ApiResponse<?> get(@PathVariable long taskId) { return tasks.find(taskId).<ApiResponse<?>>map(ApiResponse::ok).orElseGet(() -> ApiResponse.failure("TASK_NOT_FOUND", "Task not found")); }
 @PostMapping("/{taskId}/cancel") public ApiResponse<?> cancel(@PathVariable long taskId) { return tasks.update(taskId, AgentTaskStatus.CANCELLED).<ApiResponse<?>>map(ApiResponse::ok).orElseGet(() -> ApiResponse.failure("TASK_NOT_FOUND", "Task not found")); }
 @PostMapping("/{taskId}/confirm") public ApiResponse<?> confirm(@PathVariable long taskId) { return tasks.update(taskId, AgentTaskStatus.EXECUTING).<ApiResponse<?>>map(ApiResponse::ok).orElseGet(() -> ApiResponse.failure("TASK_NOT_FOUND", "Task not found")); }
 @PostMapping("/{taskId}/rollback") public ApiResponse<?> rollback(@PathVariable long taskId) { return tasks.update(taskId, AgentTaskStatus.ROLLED_BACK).<ApiResponse<?>>map(ApiResponse::ok).orElseGet(() -> ApiResponse.failure("TASK_NOT_FOUND", "Task not found")); }
 @GetMapping("/{taskId}/metrics") public ApiResponse<Map<String, Object>> metrics(@PathVariable long taskId) { var events = traces.findByTask(taskId); return ApiResponse.ok(Map.of("taskId", taskId, "stepCount", events.size(), "toolCount", events.stream().filter(e -> e.type().contains("TOOL")).count(), "testCount", events.stream().filter(e -> e.type().contains("TEST")).count(), "repairCount", events.stream().filter(e -> e.type().contains("REPAIR")).count())); }
}
