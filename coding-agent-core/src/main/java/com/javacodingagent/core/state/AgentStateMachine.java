package com.javacodingagent.core.state;

import com.javacodingagent.common.domain.AgentTaskStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/** Central transition policy; orchestration must not bypass it. */
public final class AgentStateMachine {
    private static final Map<AgentTaskStatus, EnumSet<AgentTaskStatus>> TRANSITIONS = new EnumMap<>(AgentTaskStatus.class);
    static {
        TRANSITIONS.put(AgentTaskStatus.PENDING, EnumSet.of(AgentTaskStatus.SCANNING, AgentTaskStatus.CANCELLED));
        TRANSITIONS.put(AgentTaskStatus.SCANNING, EnumSet.of(AgentTaskStatus.INDEXING, AgentTaskStatus.PLANNING, AgentTaskStatus.FAILED));
        TRANSITIONS.put(AgentTaskStatus.INDEXING, EnumSet.of(AgentTaskStatus.PLANNING, AgentTaskStatus.FAILED));
        TRANSITIONS.put(AgentTaskStatus.PLANNING, EnumSet.of(AgentTaskStatus.RETRIEVING, AgentTaskStatus.WAITING_CONFIRMATION, AgentTaskStatus.FAILED));
        TRANSITIONS.put(AgentTaskStatus.RETRIEVING, EnumSet.of(AgentTaskStatus.EXECUTING, AgentTaskStatus.FAILED));
        TRANSITIONS.put(AgentTaskStatus.EXECUTING, EnumSet.of(AgentTaskStatus.REVIEWING, AgentTaskStatus.WAITING_CONFIRMATION, AgentTaskStatus.FAILED));
        TRANSITIONS.put(AgentTaskStatus.REVIEWING, EnumSet.of(AgentTaskStatus.TESTING, AgentTaskStatus.REPAIRING, AgentTaskStatus.WAITING_CONFIRMATION));
        TRANSITIONS.put(AgentTaskStatus.TESTING, EnumSet.of(AgentTaskStatus.SUCCEEDED, AgentTaskStatus.REPAIRING, AgentTaskStatus.FAILED));
        TRANSITIONS.put(AgentTaskStatus.REPAIRING, EnumSet.of(AgentTaskStatus.RETRIEVING, AgentTaskStatus.FAILED, AgentTaskStatus.WAITING_CONFIRMATION));
        TRANSITIONS.put(AgentTaskStatus.WAITING_CONFIRMATION, EnumSet.of(AgentTaskStatus.EXECUTING, AgentTaskStatus.ROLLED_BACK, AgentTaskStatus.CANCELLED));
    }
    public boolean canTransition(AgentTaskStatus from, AgentTaskStatus to) { return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(AgentTaskStatus.class)).contains(to); }
    public void requireTransition(AgentTaskStatus from, AgentTaskStatus to) { if (!canTransition(from, to)) throw new IllegalStateException("Illegal task transition: " + from + " -> " + to); }
}
