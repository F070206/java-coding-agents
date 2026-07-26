package com.javacodingagent.bootstrap.security;
import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class JwtServiceTest { @Test void issuesAndVerifiesToken() { var service = new JwtService("12345678901234567890123456789012"); assertEquals("developer", service.verify(service.issue("developer"))); } @Test void rejectsShortConfiguredSecret() { assertThrows(IllegalArgumentException.class, () -> new JwtService("short")); } }
