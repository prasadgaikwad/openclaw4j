package dev.prasadgaikwad.openclaw4j.tool.memory;

import dev.prasadgaikwad.openclaw4j.memory.MemoryService;
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

    public MemoryTool(MemoryService memoryService) {
        this.memoryService = memoryService;
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
