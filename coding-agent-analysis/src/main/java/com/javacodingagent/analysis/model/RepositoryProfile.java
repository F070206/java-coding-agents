package com.javacodingagent.analysis.model;
import java.util.List;
public record RepositoryProfile(String rootPath, List<String> modules, List<String> technologies, int javaFileCount) { }
