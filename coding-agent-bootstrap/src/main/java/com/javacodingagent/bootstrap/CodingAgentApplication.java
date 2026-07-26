package com.javacodingagent.bootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages = "com.javacodingagent")
public class CodingAgentApplication { public static void main(String[] args) { SpringApplication.run(CodingAgentApplication.class, args); } }
