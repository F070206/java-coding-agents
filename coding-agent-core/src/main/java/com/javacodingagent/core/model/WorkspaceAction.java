package com.javacodingagent.core.model;

/** One model-selected operation against the current repository, followed by tool feedback. */
public record WorkspaceAction(Type type, String path, String query, Integer startLine,
                              Integer endLine, ChangeProposal proposal) {
 public enum Type { LIST_FILES, SEARCH_FILES, READ_FILE, APPLY_CHANGES, FINISH }
}
