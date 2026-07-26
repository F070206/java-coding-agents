package com.javacodingagent.rag;
import com.javacodingagent.analysis.JavaSourceAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class IncrementalCodeIndexerTest {
 @TempDir Path temp;
 @Test void indexesOnlyChangedFilesAndRetrievesSymbols() throws Exception { Path source = temp.resolve("UserService.java"); Files.writeString(source, "package demo; class UserService { String login() { return \"ok\"; } }"); var store = new InMemoryCodeIndexStore(); var indexer = new IncrementalCodeIndexer(new JavaSourceAnalyzer(), new CodeChunker(), store); assertEquals(1, indexer.index("repo-1", "app", temp)); assertEquals(0, indexer.index("repo-1", "app", temp)); assertFalse(new HybridCodeRetriever(store).search("repo-1", "UserService", 5).isEmpty()); Files.writeString(source, "package demo; class UserService { String login() { return \"changed\"; } }"); assertEquals(1, indexer.index("repo-1", "app", temp)); }
}
