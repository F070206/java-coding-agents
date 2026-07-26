package com.javacodingagent.rag;
import com.javacodingagent.rag.model.CodeChunk;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public class InMemoryCodeIndexStore implements CodeIndexStore {
 private final Map<String, List<CodeChunk>> chunks = new ConcurrentHashMap<>();
 private String key(String repositoryId, String filePath) { return repositoryId + "::" + filePath; }
 public List<CodeChunk> findByFile(String repositoryId, String filePath) { return List.copyOf(chunks.getOrDefault(key(repositoryId, filePath), List.of())); }
 public void replaceFile(String repositoryId, String filePath, Collection<CodeChunk> value) { chunks.put(key(repositoryId, filePath), List.copyOf(value)); }
 public List<CodeChunk> all(String repositoryId) { return chunks.entrySet().stream().filter(e -> e.getKey().startsWith(repositoryId + "::")).flatMap(e -> e.getValue().stream()).toList(); }
}
