package com.javacodingagent.rag;
import com.javacodingagent.rag.model.*;
import java.util.*;
import java.util.stream.Stream;

/** Deterministic lexical/symbol retrieval. Milvus vector scores will be merged through this boundary in the infrastructure stage. */
public class HybridCodeRetriever {
 private final CodeIndexStore store;
 public HybridCodeRetriever(CodeIndexStore store) { this.store = store; }
 public List<SearchResult> search(String repositoryId, String query, int limit) {
   String term = query.toLowerCase(Locale.ROOT);
   return store.all(repositoryId).stream().flatMap(chunk -> { double score = score(chunk, term); return score == 0 ? Stream.empty() : Stream.of(new SearchResult(chunk, score, reason(chunk, term))); })
       .sorted(Comparator.comparingDouble(SearchResult::score).reversed().thenComparing(r -> r.chunk().filePath())).limit(limit).toList();
 }
 private double score(CodeChunk chunk, String term) { double score = 0; if (chunk.symbolName().toLowerCase(Locale.ROOT).equals(term)) score += 100; else if (chunk.symbolName().toLowerCase(Locale.ROOT).contains(term)) score += 50; if (chunk.filePath().toLowerCase(Locale.ROOT).contains(term)) score += 20; if (chunk.content().toLowerCase(Locale.ROOT).contains(term)) score += 10; return score; }
 private String reason(CodeChunk chunk, String term) { return chunk.symbolName().equalsIgnoreCase(term) ? "exact-symbol" : chunk.filePath().toLowerCase(Locale.ROOT).contains(term) ? "file-path" : "content"; }
}
