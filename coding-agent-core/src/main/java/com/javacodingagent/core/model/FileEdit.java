package com.javacodingagent.core.model;
public record FileEdit(Operation operation, String filePath, String expected, String replacement, String content) { public enum Operation { CREATE, PATCH } }
