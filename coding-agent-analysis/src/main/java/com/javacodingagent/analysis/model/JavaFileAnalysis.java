package com.javacodingagent.analysis.model;
import java.util.List;
public record JavaFileAnalysis(String filePath, String packageName, List<String> imports, List<CodeSymbol> symbols) { }
