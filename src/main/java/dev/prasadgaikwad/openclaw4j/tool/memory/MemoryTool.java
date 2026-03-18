package dev.prasadgaikwad.openclaw4j.tool.memory;

import dev.prasadgaikwad.openclaw4j.memory.MemoryService;
import dev.prasadgaikwad.openclaw4j.memory.ProfileService;
import dev.prasadgaikwad.openclaw4j.tool.AITool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * A tool that allows the agent to curate its own long-term memory and log
 * events.
 *
 * <p>
 * This tool empowers the agent to persist important facts, user preferences,
 * and session decisions into the layered memory system.
 * </p>
 *
 * @author Prasad Gaikwad
 */
@Component
public class MemoryTool implements AITool {

    private static final Logger log = LoggerFactory.getLogger(MemoryTool.class);
    private final MemoryService memoryService;
    private final ProfileService profileService;

    public MemoryTool(MemoryService memoryService, ProfileService profileService) {
        this.memoryService = memoryService;
        this.profileService = profileService;
    }

    /**
     * Curates a fact or preference into long-term memory (MEMORY.md).
     *
     * @param fact the fact or preference to remember for future sessions
     * @return a confirmation message
     */
    @Tool(description = "Saves an important fact or user preference into long-term memory for future recall.")
    public String remember(String fact) {
        log.info("Agent requested to remember: {}", fact);
        memoryService.remember(fact);
        return "I have successfully saved that to my long-term memory. I will recall it in our future conversations.";
    }

    /**
     * Updates a specific user preference in the profile (USER.md).
     *
     * @param key   the preference key
     * @param value the preference value
     * @return a confirmation message
     */
    @Tool(description = "Updates a specific user preference in the profile (e.g., tone, notification settings).")
    public String updateUserPreference(String key, String value) {
        log.info("Agent requested to update preference: {}={}", key, value);
        profileService.updatePreference(key, value);
        return "I have updated your preferences in my profile.";
    }

    /**
     * Updates the agent's soul/personality definition (SOUL.md).
     *
     * @param soulContent the new personality or core behavioral instructions
     * @return a confirmation message
     */
    @Tool(description = "Updates the agent's core personality and soul definition. Use this to change how the agent behaves and responds.")
    public String updateSoul(String soulContent) {
        log.info("Agent requested to update soul");
        profileService.updateSoul(soulContent);
        return "My core soul and personality have been updated. You may notice a change in my behavior going forward.";
    }

    /**
     * Adds an environmental fact to the TOOLS.md file.
     *
     * @param fact the fact about the environment or tools to remember
     * @return a confirmation message
     */
    @Tool(description = "Adds an environmental fact to my records (e.g., repository names, server URLs).")
    public String updateEnvironmentFact(String fact) {
        log.info("Agent requested to update environment fact: {}", fact);
        profileService.updateEnvironmentFact(fact);
        return "I have recorded this environmental fact in my tool knowledge base.";
    }

    /**
     * Logs an event to the daily log file.
     *
     * @param event the event description to log
     * @return a confirmation message
     */
    @Tool(description = "Logs a raw event or scratch note to the daily log file.")
    public String logEvent(String event) {
        log.info("Agent requested to log event: {}", event);
        memoryService.logEvent(event);
        return "Event has been logged to my daily scratchpad.";
    }

    /**
     * Searches long-term memory for specific keywords.
     *
     * @param query the keyword to search
     * @return matching facts or a not-found message
     */
    @Tool(description = "Searches long-term memory (MEMORY.md) for lines matching a keyword or phrase.")
    public String searchMemory(String query) {
        log.info("Agent searching memory for: {}", query);
        java.util.List<String> results = memoryService.searchMemory(query);
        if (results.isEmpty()) {
            return "No matching memories found for: " + query;
        }
        return "Found " + results.size() + " matching memories:\n" + String.join("\n", results);
    }

    /**
     * Lists available dates in the daily history log.
     *
     * @return a formatted list of dates
     */
    @Tool(description = "Lists all dates for which a daily history log exists.")
    public String listHistory() {
        log.info("Agent requested history dates");
        java.util.List<String> dates = memoryService.listHistoryDates();
        if (dates.isEmpty()) {
            return "No history logs found.";
        }
        return "Available history logs for dates:\n" + String.join("\n", dates);
    }

    /**
     * Reads the history log for a specific date.
     *
     * @param date the date in YYYY-MM-DD format
     * @return the log content or a not-found message
     */
    @Tool(description = "Reads the content of the daily history log for a specific date (YYYY-MM-DD).")
    public String readHistoryLog(String date) {
        log.info("Agent reading history for date: {}", date);
        return memoryService.getHistoryLog(date)
                .orElse("No history log found for date: " + date);
    }

    /**
     * Removes a fact from memory.
     *
     * @param fact the fact string to match and remove
     * @return success or failure message
     */
    @Tool(description = "Removes a specific fact from long-term memory (MEMORY.md). Use this to clean up obsolete info.")
    public String forgetFact(String fact) {
        log.info("Agent requested to forget fact: {}", fact);
        if (memoryService.forgetFact(fact)) {
            return "I have removed that fact from my long-term memory.";
        } else {
            return "I could not find a fact matching '" + fact + "' in my memory.";
        }
    }

    /**
     * Updates an existing fact in memory.
     *
     * @param oldFact the existing fact to replace
     * @param newFact the new updated fact
     * @return success or failure message
     */
    @Tool(description = "Replaces an existing fact in long-term memory with a new one. Use to correct or update facts.")
    public String updateFact(String oldFact, String newFact) {
        log.info("Agent requested to update fact '{}' to '{}'", oldFact, newFact);
        if (memoryService.updateFact(oldFact, newFact)) {
            return "I have successfully updated that fact in my long-term memory.";
        } else {
            return "I could not find the old fact '" + oldFact + "' in my memory to update.";
        }
    }
}
