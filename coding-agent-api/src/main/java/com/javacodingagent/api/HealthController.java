package com.javacodingagent.api;
import com.javacodingagent.common.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
@RestController @RequestMapping("/api")
public class HealthController { @GetMapping("/health") public ApiResponse<Map<String, String>> health() { return ApiResponse.ok(Map.of("service", "java-coding-agent", "status", "UP")); } }
