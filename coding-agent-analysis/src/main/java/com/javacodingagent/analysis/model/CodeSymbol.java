package com.javacodingagent.analysis.model;

import java.util.List;

/** AST-derived symbol suitable for deterministic retrieval. */
public record CodeSymbol(String packageName, String filePath, String name, SymbolType type,
                         int startLine, int endLine, String signature, List<String> annotations,
                         List<String> references) { }
