package dev.prasadgaikwad.openclaw4j.tool.datetime;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import dev.prasadgaikwad.openclaw4j.tool.AITool;

@Component
public class DateTimeTools implements AITool {
    private static final Logger logger = LoggerFactory.getLogger(DateTimeTools.class);

    @Tool(description = "Get the current date and time in the user's timezone")
    public String getCurrentDateTime() {
        logger.info("Getting current date and time");
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }
}