package com.javacodingagent.rag.model;
public record SearchResult(CodeChunk chunk, double score, String matchReason) { }
