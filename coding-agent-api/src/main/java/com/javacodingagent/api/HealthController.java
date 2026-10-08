package com.javacodingagent.api;
import com.javacodingagent.common.web.ApiResponse;
import com.javacodingagent.core.llm.CodingModel;
import com.javacodingagent.core.llm.DeterministicCodingModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
@RestController @RequestMapping("/api")
public class HealthController {
 private final CodingModel model;
 public HealthController(CodingModel model) { this.model = model; }
 @GetMapping("/health") public ApiResponse<Map<String, String>> health() { return ApiResponse.ok(Map.of("service", "java-coding-agent", "status", "UP", "mode", model instanceof DeterministicCodingModel ? "OFFLINE" : "ONLINE")); }
}
