package com.javacodingagent.infrastructure.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javacodingagent.core.llm.DeterministicCodingModel;
import com.javacodingagent.core.model.WorkspaceAction;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class OpenAiCompatibleCodingModelTest {
 @Test void sendsLiveToolFeedbackAndDecodesActionsThroughTheOpenAiAdapter() throws Exception {
  ObjectMapper mapper = new ObjectMapper(); AtomicReference<String> request = new AtomicReference<>();
  AtomicReference<String> reply = new AtomicReference<>("""
    {"type":"READ_FILE","path":"src/Demo.java","query":null,"startLine":2,"endLine":8,"proposal":null}
    """);
  var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
  server.createContext("/v1/chat/completions", exchange -> {
   request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
   byte[] response = mapper.writeValueAsBytes(Map.of("id", "test", "object", "chat.completion", "created", 1, "model", "test-model",
     "choices", List.of(Map.of("index", 0, "message", Map.of("role", "assistant", "content", reply.get()), "finish_reason", "stop"))));
   exchange.getResponseHeaders().add("Content-Type", "application/json");
   exchange.sendResponseHeaders(200, response.length);
   try (var out = exchange.getResponseBody()) { out.write(response); }
  });
  server.start();
  try {
   var model = new OpenAiCompatibleCodingModel("http://127.0.0.1:" + server.getAddress().getPort() + "/v1", "local-test-key", "test-model", mapper);
   var plan = new DeterministicCodingModel().plan("fix", List.of());
   var action = model.nextAction(plan, List.of("SEARCH_FILES OK: src/Demo.java:2:currentValue"), null);
   assertEquals(WorkspaceAction.Type.READ_FILE, action.type()); assertEquals("src/Demo.java", action.path()); assertEquals(2, action.startLine());
   String prompt = mapper.readTree(request.get()).path("messages").toString();
   assertTrue(prompt.contains("SEARCH_FILES OK: src/Demo.java:2:currentValue"));
   assertTrue(prompt.contains("APPLY_CHANGES")); assertTrue(prompt.contains("40 tool steps"));
   reply.set("""
     {"type":"APPLY_CHANGES","proposal":{"explanation":"fix","requiresConfirmation":false,"edits":[{"operation":"PATCH","filePath":"src/Demo.java","expected":"old","replacement":"new","content":null}]}}
     """);
   var change = model.nextAction(plan, List.of("READ_FILE OK: old"), null);
   assertEquals(WorkspaceAction.Type.APPLY_CHANGES, change.type()); assertEquals("new", change.proposal().edits().get(0).replacement());
   reply.set("not a structured action");
   assertEquals("MODEL_DECISION_FAILED", assertThrows(IllegalStateException.class, () -> model.nextAction(plan, List.of(), null)).getMessage());
  } finally { server.stop(0); }
 }
}
