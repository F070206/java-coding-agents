package com.javacodingagent.infrastructure.git;
import org.eclipse.jgit.api.Git; import java.nio.file.Path;
public class JGitRepositoryClient { public String status(Path root) { try (Git git = Git.open(root.toFile())) { return git.status().call().toString(); } catch (Exception e) { throw new IllegalStateException("Cannot read Git repository", e); } } public String head(Path root) { try (Git git = Git.open(root.toFile())) { var id = git.getRepository().resolve("HEAD"); return id == null ? null : id.name(); } catch (Exception e) { throw new IllegalStateException("Cannot resolve HEAD", e); } } }
