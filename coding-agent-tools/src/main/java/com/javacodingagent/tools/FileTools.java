package com.javacodingagent.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;

/** File operations only accept workspace-relative paths and retain original content for rollback. */
public class FileTools {
 private final WorkspaceGuard guard; private final Path snapshotRoot;
 public FileTools(WorkspaceGuard guard, Path snapshotRoot) { this.guard = guard; this.snapshotRoot = snapshotRoot; }
 public ToolResult<String> read(String path, Integer startLine, Integer endLine) {
  long started = System.currentTimeMillis();
  try {
   Path file = guard.resolve(path);
   if (Files.size(file) > 1_048_576) throw new IllegalArgumentException("File exceeds the 1 MiB read limit");
   String text = Files.readString(file);
   if (text.indexOf('\0') >= 0) throw new IllegalArgumentException("Binary files cannot be read as source");
   // Keep the original line endings so exact patches also work on Windows repositories.
   String[] lines = text.isEmpty() ? new String[0] : text.split("(?<=\n)");
   int from = startLine == null ? 1 : startLine;
   if (from < 1 || (endLine != null && endLine < from) || from > Math.max(1, lines.length))
    throw new IllegalArgumentException("Invalid line range");
   int to = (int)Math.min(lines.length, Math.min((long)from + 399, endLine == null ? lines.length : endLine));
   StringBuilder result = new StringBuilder(); int last = from - 1;
   for (int i = from - 1; i < to; i++) {
    if (result.length() + lines[i].length() > 24_000) {
     if (result.isEmpty()) throw new IllegalArgumentException("Source line exceeds the 24000 character read limit");
     break;
    }
    result.append(lines[i]); last = i + 1;
   }
   return ToolResult.success("FileReadTool", "read " + path + " lines " + from + "-" + last + " of " + lines.length
     + (last < lines.length ? "; more lines available using startLine=" + (last + 1) : ""), result.toString(), started);
  } catch (Exception e) { return ToolResult.failure("FileReadTool", "FILE_READ_FAILED", e.getMessage(), started); }
 }
 public ToolResult<String> create(String path, String content) { return write("FileCreateTool", path, content, false); }
 public ToolResult<String> patch(String path, String expected, String replacement) { long started = System.currentTimeMillis(); try { Path file = guard.resolve(path); if (expected == null || expected.isEmpty() || replacement == null) throw new IllegalArgumentException("Patch requires nonempty expected text and a replacement"); String before = Files.readString(file); if (!before.contains(expected)) return ToolResult.failure("FilePatchTool", "PATCH_TARGET_NOT_FOUND", "Expected text does not occur exactly once", started); if (before.indexOf(expected) != before.lastIndexOf(expected)) return ToolResult.failure("FilePatchTool", "PATCH_TARGET_AMBIGUOUS", "Expected text occurs more than once", started); backup(file); Files.writeString(file, before.replace(expected, replacement), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING); return ToolResult.success("FilePatchTool", "patched " + path, path, started); } catch (Exception e) { return ToolResult.failure("FilePatchTool", "FILE_PATCH_FAILED", e.getMessage(), started); } }
 public ToolResult<String> delete(String path) { long started = System.currentTimeMillis(); try { Path file = guard.resolve(path); backup(file); Files.delete(file); return ToolResult.success("FileDeleteTool", "deleted " + path, path, started); } catch (Exception e) { return ToolResult.failure("FileDeleteTool", "FILE_DELETE_FAILED", e.getMessage(), started); } }
 public ToolResult<Integer> rollback() { long started = System.currentTimeMillis(); int[] restored = {0}; try { if (!Files.exists(snapshotRoot)) return ToolResult.success("RollbackTool", "no snapshot", 0, started); try (var paths = Files.walk(snapshotRoot)) { paths.filter(Files::isRegularFile).forEach(snapshot -> { try { Path target = guard.root().resolve(snapshotRoot.relativize(snapshot).toString().replace(".bak", "")).normalize(); Files.createDirectories(target.getParent()); Files.copy(snapshot, target, StandardCopyOption.REPLACE_EXISTING); restored[0]++; } catch (IOException e) { throw new IllegalStateException(e); } }); } return ToolResult.success("RollbackTool", "restored files", restored[0], started); } catch (Exception e) { return ToolResult.failure("RollbackTool", "ROLLBACK_FAILED", e.getMessage(), started); } }
 private ToolResult<String> write(String tool, String path, String content, boolean overwrite) { long started = System.currentTimeMillis(); try { Path file = guard.resolve(path); if (Files.exists(file) && !overwrite) return ToolResult.failure(tool, "FILE_EXISTS", "Refusing to overwrite existing file", started); Files.createDirectories(file.getParent()); Files.writeString(file, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW); return ToolResult.success(tool, "created " + path, path, started); } catch (Exception e) { return ToolResult.failure(tool, "FILE_CREATE_FAILED", e.getMessage(), started); } }
 private void backup(Path file) throws IOException { if (!Files.exists(file)) return; Path backup = snapshotRoot.resolve(guard.root().relativize(file).toString() + ".bak"); Files.createDirectories(backup.getParent()); if (!Files.exists(backup)) Files.copy(file, backup); }
}
