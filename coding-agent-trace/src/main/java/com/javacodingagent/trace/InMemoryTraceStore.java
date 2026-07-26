package com.javacodingagent.trace;
import java.util.*; import java.util.concurrent.CopyOnWriteArrayList;
public class InMemoryTraceStore implements TraceStore { private final List<TraceEvent> events = new CopyOnWriteArrayList<>(); public void append(TraceEvent event) { events.add(event); } public List<TraceEvent> findByTask(Long taskId) { return events.stream().filter(e -> Objects.equals(e.taskId(), taskId)).toList(); } }
