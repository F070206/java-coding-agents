package com.javacodingagent.tools;
import java.util.*; import java.util.concurrent.ConcurrentHashMap;
/** Central registry used by function-calling adapters to expose only approved tools. */
public class ToolRegistry { private final Map<String, Object> tools = new ConcurrentHashMap<>(); public ToolRegistry register(String name, Object tool) { if (name == null || name.isBlank() || tool == null) throw new IllegalArgumentException("Tool name and instance are required"); if (tools.putIfAbsent(name, tool) != null) throw new IllegalStateException("Duplicate tool: " + name); return this; } public Optional<Object> find(String name) { return Optional.ofNullable(tools.get(name)); } public Set<String> names() { return Collections.unmodifiableSet(tools.keySet()); } }
