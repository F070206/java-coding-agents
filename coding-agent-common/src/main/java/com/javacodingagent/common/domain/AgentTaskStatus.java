package com.javacodingagent.common.domain;

/** The only legal lifecycle statuses of a coding task. */
public enum AgentTaskStatus { PENDING, SCANNING, INDEXING, PLANNING, RETRIEVING, EXECUTING, REVIEWING, TESTING, REPAIRING, SUCCEEDED, FAILED, CANCELLED, WAITING_CONFIRMATION, ROLLED_BACK }
