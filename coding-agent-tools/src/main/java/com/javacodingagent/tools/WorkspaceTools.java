package com.javacodingagent.tools;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/** Live file discovery; source contents are never indexed or copied to a retrieval store. */
public final class WorkspaceTools {
 private static final Set<String> EXCLUDED = Set.of(".git", ".agent-snapshots", ".idea", ".m2-repository", "target", "build", "node_modules", "dist", ".gradle");
 private static final int MAX_RESULTS = 200;
 private final WorkspaceGuard guard;
 public WorkspaceTools(WorkspaceGuard guard) { this.guard = guard; }

 public ToolResult<String> list(String path, String query) { return discover(path, query, false); }
 public ToolResult<String> search(String path, String query) { return discover(path, query, true); }

 private ToolResult<String> discover(String path, String query, boolean search) {
  long started = System.currentTimeMillis(); String tool = search ? "FileSearchTool" : "FileListTool";
  try {
   if (search && (query == null || query.isBlank())) throw new IllegalArgumentException("Search query must not be empty");
   Path base = guard.resolve(path == null || path.isBlank() ? "." : path);
   List<String> results = new ArrayList<>(); int[] visited = {0}; int[] skipped = {0}; boolean[] truncated = {false};
   Files.walkFileTree(base, new SimpleFileVisitor<>() {
    @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
     if (!dir.equals(guard.root()) && (EXCLUDED.contains(dir.getFileName().toString()) || !allowed(dir))) return FileVisitResult.SKIP_SUBTREE;
     if (++visited[0] > 20_000) { truncated[0] = true; return FileVisitResult.TERMINATE; }
     return FileVisitResult.CONTINUE;
    }
    @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
     if (++visited[0] > 20_000) { truncated[0] = true; return FileVisitResult.TERMINATE; }
     if (!attrs.isRegularFile() || !allowed(file)) return FileVisitResult.CONTINUE;
     String relative = guard.root().relativize(file).toString().replace('\\', '/');
     if (!search) { if (query == null || query.isBlank() || relative.contains(query)) results.add(relative); }
     else if (attrs.size() > 1_048_576) skipped[0]++;
     else {
      try (var reader = Files.newBufferedReader(file)) {
       String line; int number = 0;
       while ((line = reader.readLine()) != null) {
        number++;
        if (line.indexOf('\0') >= 0) { skipped[0]++; break; }
        if (line.contains(query)) {
         int match = line.indexOf(query), from = Math.max(0, match - 80);
         results.add(relative + ":" + number + ":" + line.substring(from, Math.min(line.length(), from + 300)));
         if (results.size() >= MAX_RESULTS) break;
        }
       }
      } catch (MalformedInputException e) { skipped[0]++; }
     }
     if (results.size() >= MAX_RESULTS) { truncated[0] = true; return FileVisitResult.TERMINATE; }
     return FileVisitResult.CONTINUE;
    }
   });
   Collections.sort(results);
   String summary = results.size() + " results" + (truncated[0] ? "; truncated, narrow the path/query" : "")
     + (skipped[0] > 0 ? "; skipped " + skipped[0] + " binary/non-UTF-8/large files" : "");
   return ToolResult.success(tool, summary, String.join("\n", results), started);
  } catch (Exception e) { return ToolResult.failure(tool, "WORKSPACE_SEARCH_FAILED", e.getMessage(), started); }
 }
 private boolean allowed(Path file) {
  try { guard.resolve(guard.root().relativize(file).toString()); return true; }
  catch (SecurityException e) { return false; }
 }
}
