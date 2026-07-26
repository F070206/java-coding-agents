package com.javacodingagent.trace;
import java.time.Instant;
import java.util.Map;
/** Safe display data only; hidden reasoning and secrets never belong here. */
public record TraceEvent(String traceId, Long taskId, long sequence, String type, String role, String summary, Map<String, String> attributes, Instant occurredAt) { }
