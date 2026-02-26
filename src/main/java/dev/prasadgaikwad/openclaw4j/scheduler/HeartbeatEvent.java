package dev.prasadgaikwad.openclaw4j.scheduler;

import java.time.Instant;
import java.util.Map;

/**
 * Event published periodically by the HeartbeatMonitor to indicate background
 * activity.
 */
public record HeartbeatEvent(Instant timestamp, Map<String, Object> status) {
}
