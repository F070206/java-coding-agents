package com.javacodingagent.bootstrap.security;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javacodingagent.common.web.ApiResponse;
import org.springframework.beans.factory.annotation.Value; import org.springframework.context.annotation.*; import org.springframework.security.config.annotation.web.builders.HttpSecurity; import org.springframework.security.config.http.SessionCreationPolicy; import org.springframework.security.web.*; import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration public class SecurityConfig { @Bean JwtService jwtService(@Value("${coding-agent.security.jwt-secret:}") String secret) { return new JwtService(secret); } @Bean SecurityFilterChain security(HttpSecurity http, JwtService jwt, ObjectMapper mapper) throws Exception { return http.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(a -> a.requestMatchers("/", "/index.html", "/app.css", "/app.js", "/favicon.ico", "/api/health", "/api/auth/token", "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll().anyRequest().authenticated()).exceptionHandling(e -> e.authenticationEntryPoint((request, response, exception) -> {
  response.setStatus(401);
  response.setContentType("application/json");
  response.setCharacterEncoding("UTF-8");
  mapper.writeValue(response.getOutputStream(), ApiResponse.failure("UNAUTHORIZED", "登录已失效，请重新登录。"));
 })).addFilterBefore(new JwtAuthenticationFilter(jwt), UsernamePasswordAuthenticationFilter.class).build(); } }
