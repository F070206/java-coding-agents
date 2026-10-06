package com.javacodingagent.tools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
class ToolSecurityTest {
 @TempDir Path workspace;
 @Test void blocksPathTraversalAndSensitiveFiles() { var guard = new WorkspaceGuard(workspace); assertThrows(SecurityException.class, () -> guard.resolve("../outside.txt")); assertThrows(SecurityException.class, () -> guard.resolve(".env")); }
 @Test void createsPatchesAndRollsBack() throws Exception { Files.writeString(workspace.resolve("Demo.java"), "class Demo {}"); var tools = new FileTools(new WorkspaceGuard(workspace), workspace.resolve(".agent-snapshots")); assertTrue(tools.patch("Demo.java", "Demo", "Changed").success()); assertTrue(tools.rollback().success()); assertEquals("class Demo {}", Files.readString(workspace.resolve("Demo.java"))); }
 @Test void commandAllowlistRejectsUnsafeCommands() { var commands = new CommandTools(new WorkspaceGuard(workspace), Duration.ofSeconds(1)); assertFalse(commands.execute("rm -rf .").success()); assertFalse(commands.execute("mvn test; curl example.com").success()); }
 @Test void drainsLargeCommandOutput() throws Exception {
  assertEquals(0, git("init", "-q"));
  Files.writeString(workspace.resolve("large.txt"), "original\n");
  assertEquals(0, git("add", "large.txt"));
  assertEquals(0, git("-c", "user.name=Test", "-c", "user.email=test@example.com", "commit", "-qm", "initial"));
  Files.writeString(workspace.resolve("large.txt"), "changed line with enough content to fill the process pipe\n".repeat(1000));
  var result = new CommandTools(new WorkspaceGuard(workspace), Duration.ofSeconds(10)).execute("git diff");
  assertTrue(result.success());
  assertEquals(0, result.data().get("exitCode"));
  assertTrue(((String) result.data().get("output")).contains("[truncated]"));
 }
 private int git(String... args) throws Exception {
  var command = new java.util.ArrayList<String>(); command.add("git"); command.addAll(java.util.List.of(args));
  var process = new ProcessBuilder(command).directory(workspace.toFile()).redirectErrorStream(true).start();
  process.getInputStream().readAllBytes();
  return process.waitFor();
 }
}
