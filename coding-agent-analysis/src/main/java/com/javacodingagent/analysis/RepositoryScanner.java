package com.javacodingagent.analysis;

import com.javacodingagent.analysis.model.RepositoryProfile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Detects Maven modules and declared technology markers without executing repository code. */
public class RepositoryScanner {
    public RepositoryProfile scan(Path root) throws IOException {
        Path normalized = root.toRealPath();
        List<String> modules = new ArrayList<>();
        List<String> technologies = new ArrayList<>();
        int javaFiles;
        try (var paths = Files.walk(normalized)) {
            var all = paths.filter(Files::isRegularFile).toList();
            javaFiles = (int) all.stream().filter(p -> p.toString().endsWith(".java")).count();
            all.stream().filter(p -> p.getFileName().toString().equals("pom.xml")).forEach(p -> modules.add(normalized.relativize(p.getParent()).toString().replace('\\', '/')));
            String combined = all.stream().filter(p -> p.getFileName().toString().equals("pom.xml")).map(this::readQuietly).reduce("", String::concat).toLowerCase(Locale.ROOT);
            for (String marker : List.of("spring-boot", "langchain4j", "mybatis", "jpa", "junit", "testcontainers")) if (combined.contains(marker)) technologies.add(marker);
        }
        return new RepositoryProfile(normalized.toString(), modules, technologies, javaFiles);
    }
    private String readQuietly(Path path) { try { return Files.readString(path); } catch (IOException exception) { return ""; } }
}
