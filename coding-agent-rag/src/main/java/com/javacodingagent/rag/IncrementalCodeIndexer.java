package com.javacodingagent.rag;

import com.javacodingagent.analysis.JavaSourceAnalyzer;
import java.io.IOException;
import java.nio.file.*;

/** Hash comparison avoids re-parsing unchanged source files between tasks. */
public class IncrementalCodeIndexer {
 private final JavaSourceAnalyzer analyzer; private final CodeChunker chunker; private final CodeIndexStore store;
 public IncrementalCodeIndexer(JavaSourceAnalyzer analyzer, CodeChunker chunker, CodeIndexStore store) { this.analyzer = analyzer; this.chunker = chunker; this.store = store; }
 public int index(String repositoryId, String moduleName, Path root) throws IOException {
   int[] updated = {0}; try (var paths = Files.walk(root)) { for (Path file : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
     String content = Files.readString(file); String hash = chunker.sha256(content); var current = store.findByFile(repositoryId, file.toString());
     if (!current.isEmpty() && current.stream().allMatch(c -> c.contentHash().equals(hash))) continue;
     store.replaceFile(repositoryId, file.toString(), chunker.chunk(repositoryId, moduleName, file, analyzer.analyze(file))); updated[0]++;
   }} return updated[0];
 }
}
