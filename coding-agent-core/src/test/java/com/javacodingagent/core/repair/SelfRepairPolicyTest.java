package com.javacodingagent.core.repair;
import com.javacodingagent.common.domain.AgentStopReason;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SelfRepairPolicyTest {
 @Test void stopsRepeatedErrorsAndNoChange() { var policy = new SelfRepairPolicy(3); assertTrue(policy.evaluate(0, "e1", "c1", false, false, false).continueRepair()); assertEquals(AgentStopReason.SAME_ERROR_REPEATED, policy.evaluate(1, "e1", "c2", false, false, false).stopReason()); }
 @Test void stopsAtLimit() { assertEquals(AgentStopReason.MAX_REPAIR_ATTEMPTS_REACHED, new SelfRepairPolicy(2).evaluate(2, "e", "c", false, false, false).stopReason()); }
}
