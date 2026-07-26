package com.javacodingagent.trace;
import org.junit.jupiter.api.Test; import java.time.Instant; import java.util.Map; import static org.junit.jupiter.api.Assertions.*;
class TraceStoreTest { @Test void separatesTasks() { var store = new InMemoryTraceStore(); store.append(new TraceEvent("t", 1L, 1, "PLAN_CREATED", "PLANNER", "plan", Map.of(), Instant.now())); assertEquals(1, store.findByTask(1L).size()); assertTrue(store.findByTask(2L).isEmpty()); } }
