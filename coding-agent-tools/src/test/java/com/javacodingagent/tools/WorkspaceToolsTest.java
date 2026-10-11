package com.javacodingagent.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceToolsTest {
 @TempDir Path root;
 private WorkspaceTools tools() { return new WorkspaceTools(new WorkspaceGuard(root)); }
 private FileTools files() { return new FileTools(new WorkspaceGuard(root), root.resolve(".agent-snapshots/test")); }
 private void write(String path, String content) throws IOException {
  Path file = root.resolve(path); Files.createDirectories(file.getParent()); Files.writeString(file, content);
 }

 @Test void searchesLiveNonJavaFilesAndSkipsPrivateAndGeneratedContent() throws Exception {
  write("src/app.js", "first\nneedle = old\n"); write("README.md", "needle docs");
  for (String path : new String[]{".env", ".git/config", ".agent-snapshots/old.java.bak", "target/Demo.java", "node_modules/lib.js"}) write(path, "needle secret");
  var listed = tools().list(".", null);
  assertTrue(listed.success()); assertEquals("README.md\nsrc/app.js", listed.data());
  var found = tools().search("src", "needle");
  assertTrue(found.success()); assertEquals("src/app.js:2:needle = old", found.data());
  write("src/app.js", "needle = current\n");
  assertTrue(tools().search("src", "current").data().contains("src/app.js:1:needle = current"));
  assertFalse(tools().search(".", "needle").data().contains("secret"));
 }

 @Test void reportsLimitsAndRejectsInvalidSearches() throws Exception {
  write("many.txt", "needle\n".repeat(205));
  var result = tools().search(".", "needle");
  assertTrue(result.summary().contains("truncated")); assertEquals(200, result.data().lines().count());
  assertFalse(tools().search(".", "").success());
  assertFalse(tools().list("../", null).success());
  assertFalse(tools().search(".env", "key").success());
  assertFalse(tools().list("missing", null).success());
 }

 @Test void readsExactLineEndingsAndBoundsRanges() throws Exception {
  write("windows.txt", "one\r\ntwo\r\nthree\r\n");
  assertEquals("two\r\n", files().read("windows.txt", 2, 2).data());
  assertTrue(files().patch("windows.txt", "two\r\n", "updated\r\n").success());
  assertEquals("one\r\nupdated\r\nthree\r\n", Files.readString(root.resolve("windows.txt")));
  assertFalse(files().read("windows.txt", 0, 1).success());
  assertFalse(files().read("windows.txt", 10, null).success());
  assertFalse(files().read("windows.txt", 2, 1).success());
  write("long.txt", "line\n".repeat(450));
  assertEquals(400, files().read("long.txt", null, null).data().lines().count());
  assertTrue(files().read("long.txt", null, null).summary().contains("startLine=401"));
  assertEquals(50, files().read("long.txt", 401, null).data().lines().count());
  write("empty.txt", ""); assertEquals("", files().read("empty.txt", null, null).data());
  assertFalse(files().patch("empty.txt", "", "unexpected").success());
 }

 @Test void rejectsBinaryAndOversizedReads() throws Exception {
  write("binary.bin", "abc\0def"); write("large.txt", "x".repeat(1_048_577));
  assertFalse(files().read("binary.bin", null, null).success());
  assertFalse(files().read("large.txt", null, null).success());
  assertTrue(tools().search(".", "abc").summary().contains("skipped 2"));
 }

 @Test void blocksSnapshotsAbsolutePathsAndSymlinkTargets() throws Exception {
  write(".agent-snapshots/secret.bak", "secret");
  assertFalse(files().read(".agent-snapshots/secret.bak", null, null).success());
  assertFalse(files().create(root.resolve("absolute.txt").toString(), "x").success());
  Path outside = Files.createTempDirectory("agent-outside-");
  try {
   Path secret = Files.writeString(outside.resolve("secret.txt"), "external");
   try { Files.createSymbolicLink(root.resolve("link.txt"), secret); Files.createSymbolicLink(root.resolve("linked-dir"), outside); }
   catch (IOException | UnsupportedOperationException e) { Assumptions.assumeTrue(false, "Symbolic links unavailable: " + e.getClass().getSimpleName()); }
   assertFalse(files().read("link.txt", null, null).success());
   assertFalse(files().patch("link.txt", "external", "changed").success());
   assertFalse(files().create("linked-dir/new/sub.txt", "x").success());
   assertEquals("external", Files.readString(secret));
  } finally {
   Files.deleteIfExists(root.resolve("link.txt")); Files.deleteIfExists(root.resolve("linked-dir"));
   Files.deleteIfExists(outside.resolve("secret.txt")); Files.deleteIfExists(outside);
  }
 }
}
