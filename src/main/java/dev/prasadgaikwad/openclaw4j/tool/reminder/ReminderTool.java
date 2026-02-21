package dev.prasadgaikwad.openclaw4j.tool.reminder;

import dev.prasadgaikwad.openclaw4j.scheduler.ReminderContext;
import dev.prasadgaikwad.openclaw4j.scheduler.ReminderEngine;
import dev.prasadgaikwad.openclaw4j.tool.AITool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Tool for the agent to set and manage time-based reminders.
 *
 * <p>
 * Channel context (channelId, threadId, source) is read from
 * {@link ReminderContext},
 * which is set per-request by {@code AgentService} before the planning cycle.
 * This means the LLM only supplies what it uniquely knows: <em>what</em> to
 * remind the user of and <em>when</em>.
 * </p>
 *
 * <h3>Design Decision: Simplified Tool Signature</h3>
 * <p>
 * Early versions asked the LLM to supply channelId, channelType, and userId
 * as explicit tool parameters. This was unreliable — the LLM sometimes supplied
 * incorrect values or left them blank. By reading these from a
 * {@link ThreadLocal}
 * context, we guarantee correctness and reduce tool call complexity.
 * </p>
 *
 * @author Prasad Gaikwad
 */
@Component
public class ReminderTool implements AITool {

    private static final Logger log = LoggerFactory.getLogger(ReminderTool.class);
    private final ReminderEngine reminderEngine;

    public ReminderTool(ReminderEngine reminderEngine) {
        this.reminderEngine = reminderEngine;
    }

    /**
     * Sets a one-time reminder.
     *
     * @param content  A short description of what to remind the user about.
     * @param remindAt ISO-8601 datetime with timezone offset, e.g.
     *                 {@code 2026-02-20T22:00:00-06:00}.
     *                 Use the "Current Time" provided in the system prompt as the
     *                 reference point.
     * @return confirmation message with reminder ID, or an error message.
     */
    @Tool(description = """
            Set a one-time reminder for the current user. \
            'content' is what to remind them about. \
            'remindAt' MUST be a full ISO-8601 datetime WITH a timezone offset (e.g. 2026-02-20T22:00:00-06:00). \
            Use the 'Current Time' from the system context as the reference for relative times like 'in 5 minutes'. \
            The channel, thread, and user details are provided automatically.
            """)
    public String setReminder(String content, String remindAt) {
        ReminderContext ctx = ReminderContext.get();
        if (ctx == null) {
            log.error("ReminderContext is not set — cannot dispatch reminder notification");
            return "Error: Reminder context is not available. Please try again.";
        }

        log.info("ReminderTool: setting reminder for user={} in channel={} at {}",
                ctx.userId(), ctx.channelId(), remindAt);

        try {
            Instant instant = OffsetDateTime.parse(remindAt).toInstant();
            String id = reminderEngine.createReminder(ctx.userId(), ctx.channelId(), ctx.threadId(), ctx.source(),
                    content, instant);
            return "✅ Reminder set successfully! I'll notify you at " + remindAt + ". (ID: " + id + ")";
        } catch (DateTimeParseException e) {
            log.error("Failed to parse reminder time: {}", remindAt, e);
            return "Error: Invalid date format '" + remindAt
                    + "'. Please use ISO-8601 with timezone offset, e.g. 2026-02-20T22:00:00-06:00.";
        } catch (Exception e) {
            log.error("Error setting reminder", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Sets a recurring reminder using a cron expression.
     *
     * @param content        A short description of what to remind the user about.
     * @param cronExpression A Spring-compatible 6-part cron expression (seconds
     *                       included),
     *                       e.g. {@code 0 0 9 * * MON} for every Monday at 9am.
     * @return confirmation message with reminder ID, or an error message.
     */
    @Tool(description = """
            Set a recurring reminder using a cron schedule. \
            'content' is what to remind them about. \
            'cronExpression' is a 6-part Spring cron expression (includes seconds field), \
            e.g. '0 0 9 * * MON' for every Monday 9am. \
            The channel, thread, and user details are provided automatically.
            """)
    public String setCronReminder(String content, String cronExpression) {
        ReminderContext ctx = ReminderContext.get();
        if (ctx == null) {
            log.error("ReminderContext is not set — cannot dispatch reminder notification");
            return "Error: Reminder context is not available. Please try again.";
        }

        log.info("ReminderTool: setting cron reminder for user={} in channel={} with pattern {}",
                ctx.userId(), ctx.channelId(), cronExpression);

        try {
            String id = reminderEngine.createCronReminder(ctx.userId(), ctx.channelId(), ctx.threadId(), ctx.source(),
                    content, cronExpression);
            return "✅ Recurring reminder set! Pattern: " + cronExpression + ". (ID: " + id + ")";
        } catch (Exception e) {
            log.error("Error setting cron reminder", e);
            return "Error: " + e.getMessage();
        }
    }
}
