package com.javacodingagent.core.repair;
import com.javacodingagent.common.domain.AgentStopReason;
public record RepairDecision(boolean continueRepair, AgentStopReason stopReason, String explanation) { }
