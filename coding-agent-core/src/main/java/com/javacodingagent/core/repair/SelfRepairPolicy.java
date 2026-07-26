package com.javacodingagent.core.repair;
import com.javacodingagent.common.domain.AgentStopReason;
import java.util.HashSet;
import java.util.Set;
/** Stateless-per-task policy object guards every repair iteration. */
public class SelfRepairPolicy {
 private final int maxAttempts; private final Set<String> errorFingerprints = new HashSet<>(); private String previousChangeFingerprint;
 public SelfRepairPolicy(int maxAttempts) { this.maxAttempts = maxAttempts; }
 public RepairDecision evaluate(int round, String errorFingerprint, String changeFingerprint, boolean unsafe, boolean contextExceeded, boolean needsConfirmation) {
  if (unsafe) return stop(AgentStopReason.UNSAFE_OPERATION_DETECTED);
  if (contextExceeded) return stop(AgentStopReason.CONTEXT_LIMIT_REACHED);
  if (needsConfirmation) return stop(AgentStopReason.HUMAN_CONFIRMATION_REQUIRED);
  if (round >= maxAttempts) return stop(AgentStopReason.MAX_REPAIR_ATTEMPTS_REACHED);
  if (!errorFingerprints.add(errorFingerprint)) return stop(AgentStopReason.SAME_ERROR_REPEATED);
  if (changeFingerprint != null && changeFingerprint.equals(previousChangeFingerprint)) return stop(AgentStopReason.NO_EFFECTIVE_CHANGE);
  previousChangeFingerprint = changeFingerprint; return new RepairDecision(true, null, "repair permitted");
 }
 private RepairDecision stop(AgentStopReason reason) { return new RepairDecision(false, reason, reason.name()); }
}
