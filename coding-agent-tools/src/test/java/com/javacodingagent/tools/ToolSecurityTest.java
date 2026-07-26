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
}
