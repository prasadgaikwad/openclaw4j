# Reminders Reference

OpenClaw4J can schedule and manage one-time or recurring reminders using an asynchronous scheduling engine.

## Features

-   **Automatic Context**: You don't need to specify the user, channel, or thread details—they're automatically read from the conversation context.
-   **Precision Scheduling**: Supports full ISO-8601 datetimes.
-   **Recurring Tasks**: Uses Spring-compatible 6-part cron expressions.

## Available Tools

#### `setReminder(String content, String remindAt)`
Sets a one-time reminder for the current user. Use this when the user says "Remind me to [content] in [duration]" or "Remind me at [time]".

-   `content`: What should the user be reminded of?
-   `remindAt`: Must be a full ISO-8601 datetime with a timezone offset (e.g. `2026-02-20T22:00:00-06:00`).

#### `setCronReminder(String content, String cronExpression)`
Sets a recurring reminder using a cron schedule. Use this when the user says "Remind me to [content] every [weekday] at [time]".

-   `cronExpression`: A Spring-compatible 6-part cron expression (e.g. `0 0 9 * * MON` for every Monday at 9am).

---

## Technical Details

Reminders are managed by a `ReminderEngine` that persists them (if configured) or executes them as scheduled tasks. Each reminder captures its originating channel and thread to ensure it's delivered back to the correct context.
