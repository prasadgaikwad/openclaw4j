package dev.prasadgaikwad.openclaw4j.scheduler;

import dev.prasadgaikwad.openclaw4j.channel.ChannelAdapter;
import dev.prasadgaikwad.openclaw4j.channel.ChannelType;
import dev.prasadgaikwad.openclaw4j.channel.OutboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Engine for managing reminders. It interacts with the SchedulerService to
 * schedule
 * notifications and uses ChannelAdapters to send them back to the user.
 */
@Service
public class ReminderEngine {

    private static final Logger log = LoggerFactory.getLogger(ReminderEngine.class);

    private final SchedulerService schedulerService;
    private final ApplicationContext applicationContext;

    public ReminderEngine(SchedulerService schedulerService, ApplicationContext applicationContext) {
        this.schedulerService = schedulerService;
        this.applicationContext = applicationContext;
    }

    /**
     * Creates a one-time reminder.
     */
    public String createReminder(String userId, String channelId, Optional<String> threadId, ChannelType source,
            String content, Instant remindAt) {
        String reminderId = "reminder-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Creating reminder {} for user {} at {}", reminderId, userId, remindAt);

        schedulerService.scheduleTask(reminderId, () -> {
            log.info("Firing reminder {}", reminderId);
            sendReminderNotification(channelId, threadId, source, content);
        }, remindAt);

        return reminderId;
    }

    /**
     * Creates a recurring reminder via cron.
     */
    public String createCronReminder(String userId, String channelId, Optional<String> threadId, ChannelType source,
            String content, String cronExpression) {
        String reminderId = "reminder-cron-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Creating cron reminder {} for user {} with pattern {}", reminderId, userId, cronExpression);

        schedulerService.scheduleCronTask(reminderId, () -> {
            log.info("Firing recurring reminder {}", reminderId);
            sendReminderNotification(channelId, threadId, source, content);
        }, cronExpression);

        return reminderId;
    }

    private void sendReminderNotification(String channelId, Optional<String> threadId, ChannelType source,
            String content) {
        // Find the appropriate adapter
        List<ChannelAdapter> adapters = applicationContext.getBeansOfType(ChannelAdapter.class).values().stream()
                .filter(a -> a.channelType().getClass().equals(source.getClass()))
                .toList();

        if (adapters.isEmpty()) {
            log.error("No channel adapter found for source type: {}", source.getClass().getSimpleName());
            return;
        }

        String reminderText = "🔔 **Reminder:** " + content;
        OutboundMessage outbound = new OutboundMessage(channelId, threadId, reminderText, source, List.of());

        adapters.forEach(adapter -> {
            try {
                adapter.sendMessage(outbound);
            } catch (Exception e) {
                log.error("Failed to send reminder notification via {}", adapter.getClass().getSimpleName(), e);
            }
        });
    }

    public void cancelReminder(String reminderId) {
        schedulerService.cancelTask(reminderId);
    }
}
