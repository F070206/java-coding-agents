package com.javacodingagent.tools;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/** Enforces one normalized repository root and blocks credential-like files. */
public final class WorkspaceGuard {
 private static final List<String> SENSITIVE_NAMES = List.of(".env", "id_rsa", "id_ed25519", ".pem", ".p12", ".key");
 private final Path root;
 public WorkspaceGuard(Path root) {
   this.root = root.toAbsolutePath().normalize();
   if (!Files.isDirectory(this.root)) throw new IllegalArgumentException("Workspace root must exist");
 }
 public Path resolve(String requested) {
   if (requested == null || requested.isBlank()) throw new SecurityException("Empty path is not allowed");
   Path candidate = root.resolve(requested).normalize();
   if (!candidate.startsWith(root)) throw new SecurityException("Path escapes workspace");
   String name = candidate.getFileName() == null ? "" : candidate.getFileName().toString().toLowerCase();
   if (SENSITIVE_NAMES.stream().anyMatch(name::contains)) throw new SecurityException("Sensitive file access is blocked");
   Path parent = candidate.getParent();
   try { if (parent != null && Files.exists(parent) && !parent.toRealPath().startsWith(root)) throw new SecurityException("Symbolic link escapes workspace"); } catch (AccessDeniedException ignored) { /* normalized-root validation remains in force */ } catch (IOException e) { throw new SecurityException("Cannot validate path", e); }
   return candidate;
 }
 public Path root() { return root; }
}
