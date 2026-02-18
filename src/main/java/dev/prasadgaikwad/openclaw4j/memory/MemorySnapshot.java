package dev.prasadgaikwad.openclaw4j.memory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A snapshot of the agent's memory at a specific point in time.
 * 
 * @param relevantMemories List of relevant memories for the current context
 * @param userPreferences  Map of user preferences
 * @param soulDirective    Optional directive from the agent's soul
 * @param toolsContext     Optional context for available tools
 */
public record MemorySnapshot(
        List<String> relevantMemories,
        Map<String, String> userPreferences,
        Optional<String> soulDirective,
        Optional<String> toolsContext) {
}
