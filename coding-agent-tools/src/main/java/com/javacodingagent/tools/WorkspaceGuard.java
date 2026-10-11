package com.javacodingagent.tools;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/** Enforces one normalized repository root and blocks credential-like files. */
public final class WorkspaceGuard {
 private static final List<String> SENSITIVE_NAMES = List.of(".env", "id_rsa", "id_ed25519", ".pem", ".p12", ".key");
 private final Path root;
 public WorkspaceGuard(Path root) {
   try { this.root = root.toRealPath(); } catch (IOException e) { throw new IllegalArgumentException("Workspace root must exist", e); }
   if (!Files.isDirectory(this.root)) throw new IllegalArgumentException("Workspace root must be a directory");
 }
 public Path resolve(String requested) {
   if (requested == null || requested.isBlank()) throw new SecurityException("Empty path is not allowed");
   if (Path.of(requested).isAbsolute()) throw new SecurityException("Use a workspace-relative path");
   Path candidate = root.resolve(requested).normalize();
   if (!candidate.startsWith(root)) throw new SecurityException("Path escapes workspace");
   for (Path segment : root.relativize(candidate)) {
     String name = segment.toString().toLowerCase(java.util.Locale.ROOT);
     if (name.equals(".git") || name.equals(".agent-snapshots") || SENSITIVE_NAMES.stream().anyMatch(name::contains))
       throw new SecurityException("Sensitive file access is blocked");
   }
   // Validate the file itself and all existing ancestors, including creation below a symlink.
   try {
     for (Path current = candidate; current != null && current.startsWith(root); current = current.getParent()) {
       if (Files.isSymbolicLink(current)) throw new SecurityException("Symbolic link access is blocked");
       if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && !current.toRealPath().startsWith(root))
         throw new SecurityException("Symbolic link escapes workspace");
     }
   } catch (IOException e) { throw new SecurityException("Cannot validate path", e); }
   return candidate;
 }
 public Path root() { return root; }
}
