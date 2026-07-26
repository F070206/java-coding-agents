package com.javacodingagent.rag.model;

import java.util.Map;

/** A semantic unit indexed independently; its boundary follows source symbols rather than a fixed character limit. */
public record CodeChunk(String repositoryId, String moduleName, String filePath, String language,
                        String packageName, String symbolName, String symbolType, int startLine,
                        int endLine, String content, String contentHash, Map<String, String> metadata) { }
