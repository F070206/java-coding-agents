package com.javacodingagent.rag;
import com.javacodingagent.rag.model.CodeChunk;
import java.util.Collection;
import java.util.List;
public interface CodeIndexStore {
    List<CodeChunk> findByFile(String repositoryId, String filePath);
    void replaceFile(String repositoryId, String filePath, Collection<CodeChunk> chunks);
    List<CodeChunk> all(String repositoryId);
}
