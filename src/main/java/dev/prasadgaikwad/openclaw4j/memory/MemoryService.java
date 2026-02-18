package dev.prasadgaikwad.openclaw4j.memory;

import java.util.List;

/**
 * Defines the contract for the agent's memory system.
 *
 * <p>
 * This service manages long-term persistence of curated memories (preferences,
 * decisions)
 * and daily logs of raw events.
 * </p>
 *
 * @author Prasad Gaikwad
 */
public interface MemoryService {

    /**
     * Loads curated memories that are relevant to the current conversation.
     *
     * @return a list of relevant memory strings
     */
    List<String> getRelevantMemories();

    /**
     * Stores a curated fact or decision into long-term memory (MEMORY.md).
     *
     * @param fact the fact to remember
     */
    void remember(String fact);

    /**
     * Logs a raw event to the daily memory log (memory/daily/YYYY-MM-DD.md).
     *
     * @param event the event to log
     */
    void logEvent(String event);
}
