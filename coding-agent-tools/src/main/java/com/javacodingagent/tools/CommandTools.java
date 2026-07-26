package com.javacodingagent.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Allows only Maven, approved read-only Git commands and java version checks. */
public class CommandTools {
 private final WorkspaceGuard guard; private final Duration timeout;
 public CommandTools(WorkspaceGuard guard, Duration timeout) { this.guard = guard; this.timeout = timeout; }
 public ToolResult<Map<String, Object>> execute(String command) { long started = System.currentTimeMillis(); try {
   List<String> args = validate(command); List<String> processArgs = platformCommand(args); Process process = new ProcessBuilder(processArgs).directory(guard.root().toFile()).redirectErrorStream(true).start();
   boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS); if (!finished) { process.destroyForcibly(); return ToolResult.failure("MavenTool", "COMMAND_TIMEOUT", "Command exceeded " + timeout, started); }
   String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8); if (output.length() > 20_000) output = output.substring(0, 20_000) + "\n[truncated]";
   return ToolResult.success(toolName(args), "command completed", Map.of("command", String.join(" ", args), "exitCode", process.exitValue(), "output", output), started);
 } catch (Exception e) { return ToolResult.failure("CommandTool", "COMMAND_REJECTED", e.getMessage(), started); } }
 private List<String> validate(String command) {
   if (command == null || command.matches(".*[;&|><`$()].*")) throw new SecurityException("Shell operators are not allowed");
   List<String> args = Arrays.asList(command.trim().split("\\s+")); if (args.isEmpty()) throw new SecurityException("Empty command");
   String first = args.get(0).toLowerCase(Locale.ROOT); boolean maven = first.equals("mvn") || first.equals("mvnw") || first.equals("./mvnw");
   boolean git = args.size() >= 2 && first.equals("git") && Set.of("status", "diff", "log", "branch").contains(args.get(1));
   boolean javaVersion = args.size() == 2 && first.equals("java") && args.get(1).equals("-version");
   if (!maven && !git && !javaVersion) throw new SecurityException("Command is not on the allowlist"); return args;
 }
 private String toolName(List<String> args) { return args.get(0).startsWith("git") ? "GitTool" : args.get(0).equals("java") ? "JavaVersionTool" : "MavenTool"; }
 private List<String> platformCommand(List<String> args) { if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") && Set.of("mvn", "mvnw", "./mvnw").contains(args.get(0).toLowerCase(Locale.ROOT))) { var result = new ArrayList<String>(); result.add("cmd"); result.add("/c"); result.addAll(args); return result; } return args; }
}
