package com.javacodingagent.trace;
import java.util.concurrent.*;
/** Trace write errors are isolated from the agent execution path. */
public class AsyncTraceWriter implements AutoCloseable { private final TraceStore store; private final ExecutorService executor = Executors.newSingleThreadExecutor(); public AsyncTraceWriter(TraceStore store) { this.store = store; } public void write(TraceEvent event) { executor.submit(() -> { try { store.append(event); } catch (RuntimeException ignored) { } }); } public void close() { executor.shutdown(); } }
