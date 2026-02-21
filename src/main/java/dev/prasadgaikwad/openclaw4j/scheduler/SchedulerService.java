package dev.prasadgaikwad.openclaw4j.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Service for managing scheduled tasks using Spring's TaskScheduler.
 * Supports both one-time delayed tasks and recurring cron-based tasks.
 */
@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final TaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public SchedulerService(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    /**
     * Schedules a one-time task to run at the specified time.
     *
     * @param taskId    unique identifier for the task
     * @param task      the task to execute
     * @param startTime the time when the task should run
     */
    @SuppressWarnings("null")
    public void scheduleTask(String taskId, Runnable task, Instant startTime) {
        log.info("Scheduling one-time task: {} at {}", taskId, startTime);
        cancelTask(taskId); // Cancel any existing task with the same ID
        ScheduledFuture<?> future = taskScheduler.schedule(() -> {
            try {
                task.run();
            } finally {
                scheduledTasks.remove(taskId);
            }
        }, startTime);
        scheduledTasks.put(taskId, future);
    }

    /**
     * Schedules a recurring task using a cron expression.
     *
     * @param taskId         unique identifier for the task
     * @param task           the task to execute
     * @param cronExpression the cron expression
     */
    @SuppressWarnings("null")
    public void scheduleCronTask(String taskId, Runnable task, String cronExpression) {
        log.info("Scheduling cron task: {} with expression: {}", taskId, cronExpression);
        cancelTask(taskId);
        ScheduledFuture<?> future = taskScheduler.schedule(task, new CronTrigger(cronExpression));
        scheduledTasks.put(taskId, future);
    }

    /**
     * Cancels a scheduled task.
     *
     * @param taskId unique identifier for the task
     */
    public void cancelTask(String taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null && !future.isDone()) {
            log.info("Cancelling task: {}", taskId);
            future.cancel(false);
        }
    }

    /**
     * Checks if a task is currently scheduled.
     *
     * @param taskId unique identifier
     * @return true if scheduled
     */
    public boolean isTaskScheduled(String taskId) {
        ScheduledFuture<?> future = scheduledTasks.get(taskId);
        return future != null && !future.isDone();
    }
}
