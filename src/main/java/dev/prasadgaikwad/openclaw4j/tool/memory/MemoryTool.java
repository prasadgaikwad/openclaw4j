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
}
