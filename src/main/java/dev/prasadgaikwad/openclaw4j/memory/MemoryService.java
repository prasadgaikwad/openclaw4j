package dev.prasadgaikwad.openclaw4j.memory;

import java.util.List;
import java.util.Optional;

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

    /**
     * Searches long-term memory (MEMORY.md) for lines matching the query.
     *
     * @param query the keyword or phrase to search for (case-insensitive)
     * @return list of matching memory lines
     */
    List<String> searchMemory(String query);

    /**
     * Lists all dates for which a daily history log exists.
     *
     * @return list of date strings in YYYY-MM-DD format
     */
    List<String> listHistoryDates();

    /**
     * Reads the content of the daily history log for the given date.
     *
     * @param date the date in YYYY-MM-DD format
     * @return the log content, or empty if no log exists for that date
     */
    Optional<String> getHistoryLog(String date);

    /**
     * Removes a specific fact from long-term memory (MEMORY.md).
     *
     * @param fact the fact to forget (matches lines containing this string)
     * @return true if at least one line was removed, false otherwise
     */
    boolean forgetFact(String fact);

    /**
     * Replaces an existing fact in long-term memory (MEMORY.md) with a new one.
     *
     * @param oldFact the existing fact to replace (matched by containment)
     * @param newFact the new fact to insert in its place
     * @return true if the fact was found and updated, false otherwise
     */
    boolean updateFact(String oldFact, String newFact);
}
