package com.javacodingagent.core.model;
public record StructuredError(String errorType, String filePath, Integer lineNumber, String symbol,
                              String probableCause, String recommendedFix, boolean autoRepairAllowed) { }
