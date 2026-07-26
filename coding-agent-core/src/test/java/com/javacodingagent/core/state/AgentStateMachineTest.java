package com.javacodingagent.core.state;
import com.javacodingagent.common.domain.AgentTaskStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AgentStateMachineTest {
 @Test void acceptsNominalTransitions() { var machine = new AgentStateMachine(); assertTrue(machine.canTransition(AgentTaskStatus.PENDING, AgentTaskStatus.SCANNING)); assertTrue(machine.canTransition(AgentTaskStatus.TESTING, AgentTaskStatus.REPAIRING)); }
 @Test void rejectsTerminalTransitions() { var machine = new AgentStateMachine(); assertFalse(machine.canTransition(AgentTaskStatus.SUCCEEDED, AgentTaskStatus.SCANNING)); assertThrows(IllegalStateException.class, () -> machine.requireTransition(AgentTaskStatus.SUCCEEDED, AgentTaskStatus.SCANNING)); }
}
