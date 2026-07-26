package com.javacodingagent.api.trace;
import com.javacodingagent.common.web.ApiResponse; import com.javacodingagent.trace.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.servlet.mvc.method.annotation.SseEmitter; import java.util.*;
@RestController @RequestMapping("/api/agent/tasks/{taskId}") public class TraceController { private final TraceStore traces; private final TaskEventBroker events; public TraceController(TraceStore traces, TaskEventBroker events) { this.traces = traces; this.events = events; }
 @GetMapping("/events") SseEmitter events(@PathVariable long taskId) { return events.subscribe(taskId); }
 @GetMapping({"/trace", "/steps"}) ApiResponse<?> trace(@PathVariable long taskId) { return ApiResponse.ok(traces.findByTask(taskId)); }
 @GetMapping("/tools") ApiResponse<?> tools(@PathVariable long taskId) { return filtered(taskId, "TOOL"); }
 @GetMapping("/changes") ApiResponse<?> changes(@PathVariable long taskId) { return filtered(taskId, "FILE"); }
 @GetMapping("/tests") ApiResponse<?> tests(@PathVariable long taskId) { return filtered(taskId, "TEST"); }
 private ApiResponse<?> filtered(long id, String prefix) { return ApiResponse.ok(traces.findByTask(id).stream().filter(e -> e.type().contains(prefix)).toList()); }
}
