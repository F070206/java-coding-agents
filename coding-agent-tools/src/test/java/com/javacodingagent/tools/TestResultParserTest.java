package com.javacodingagent.tools;
import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class TestResultParserTest { @Test void parsesSurefireSummary() { var result = new TestResultParser().parse(1, "Tests run: 5, Failures: 1, Errors: 1, Skipped: 1"); assertEquals("FAILED", result.status()); assertEquals(2, result.passedTests()); assertEquals(2, result.failedTests()); } }
