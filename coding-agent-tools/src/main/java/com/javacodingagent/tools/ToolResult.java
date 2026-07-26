package com.javacodingagent.tools;
/** Stable, machine-readable response used by every agent tool. */
public record ToolResult<T>(boolean success, String toolName, String summary, T data, String errorCode, String errorMessage, long durationMs) {
 public static <T> ToolResult<T> success(String tool, String summary, T data, long started) { return new ToolResult<>(true, tool, summary, data, null, null, System.currentTimeMillis() - started); }
 public static <T> ToolResult<T> failure(String tool, String code, String message, long started) { return new ToolResult<>(false, tool, "failed", null, code, message, System.currentTimeMillis() - started); }
}
