package com.javacodingagent.analysis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class JavaSourceAnalyzerTest {
 @TempDir Path temp;
 @Test void extractsClassMethodAnnotationsAndCalls() throws Exception { Path source = temp.resolve("Demo.java"); Files.writeString(source, "package sample; import org.springframework.web.bind.annotation.GetMapping; @Deprecated class Demo { @GetMapping String hello(String name) { return format(name); } String format(String n) { return n; } }"); var result = new JavaSourceAnalyzer().analyze(source); assertEquals("sample", result.packageName()); assertTrue(result.symbols().stream().anyMatch(s -> s.name().equals("hello") && s.annotations().contains("GetMapping") && s.references().contains("format"))); }
}
