package com.javacodingagent.rag;

import com.javacodingagent.analysis.model.CodeSymbol;
import com.javacodingagent.analysis.model.JavaFileAnalysis;
import com.javacodingagent.rag.model.CodeChunk;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

public class CodeChunker {
    public List<CodeChunk> chunk(String repositoryId, String moduleName, Path file, JavaFileAnalysis analysis) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<CodeChunk> chunks = new ArrayList<>();
        for (CodeSymbol symbol : analysis.symbols()) {
            int from = Math.max(0, symbol.startLine() - 1), to = Math.min(lines.size(), symbol.endLine());
            if (from >= to) continue;
            String content = String.join("\n", lines.subList(from, to));
            chunks.add(new CodeChunk(repositoryId, moduleName, file.toString(), "java", analysis.packageName(), symbol.name(), symbol.type().name(), symbol.startLine(), symbol.endLine(), content, sha256(content), Map.of("signature", symbol.signature())));
        }
        return chunks;
    }
    public String sha256(String value) { try { byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); return HexFormat.of().formatHex(bytes); } catch (Exception exception) { throw new IllegalStateException(exception); } }
}
