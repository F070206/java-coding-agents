package com.javacodingagent.core.model;
import java.util.List;
public record ChangeProposal(String explanation, List<FileEdit> edits, boolean requiresConfirmation) { }
