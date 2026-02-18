package dev.prasadgaikwad.openclaw4j.tool.datetime;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import dev.prasadgaikwad.openclaw4j.tool.AITool;

/**
 * A local tool providing date and time information.
 *
 * <p>
 * This tool allows the agent to regain "temporal awareness" by retrieving the
 * current
 * system time, adjusted for the user's locale/timezone where possible.
 * </p>
 *
 * <h3>Usage in LLM Prompt (Implicit):</h3>
 * <p>
 * The model will invoke {@link #getCurrentDateTime()} when the user asks
 * questions like
 * "What time is it?" or "What's today's date?".
 * </p>
 *
 * @author Prasad Gaikwad
 * @see AITool
 */
@Component
public class DateTimeTools implements AITool {
    private static final Logger logger = LoggerFactory.getLogger(DateTimeTools.class);

    @Tool(description = "Get the current date and time in the user's timezone")
    public String getCurrentDateTime() {
        logger.info("Getting current date and time");
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }
}