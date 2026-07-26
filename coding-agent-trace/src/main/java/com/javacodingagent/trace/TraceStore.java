package com.javacodingagent.trace;
import java.util.List;
public interface TraceStore { void append(TraceEvent event); List<TraceEvent> findByTask(Long taskId); }
