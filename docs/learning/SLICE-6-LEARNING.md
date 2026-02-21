# Slice 6 — Learning Guide: Scheduler & Reminders

> **What you built:** A time-based task execution system and a heartbeat monitor that allows the agent to set, manage, and fire reminders and periodic background tasks.
> **Concepts covered:** Spring `@Scheduled`, `TaskScheduler`, Cron Expressions, State Persistence (JSON), and time-aware tool calling.

---

## 1. Conceptual Model: Scheduler Architecture

| Component | Implementation | Purpose | Storage |
|-------|----------------|---------|-----------|
| **Core Scheduler** | `SchedulerService` | Low-level wrapper around Spring `TaskScheduler` | In-memory (Jobs) |
| **Reminder Engine** | `ReminderEngine` | High-level logic for creating and firing user notifications | N/A |
| **Heartbeat Monitor** | `HeartbeatMonitor` | Periodic background task for state updates and housekeeping | Persistent (JSON) |
| **Reminder Tool** | `ReminderTool` | LLM-facing tool for setting one-time or recurring reminders | N/A |

---

## 2. Task Scheduling in Spring

### 2.1 One-time vs. Recurring
OpenClaw4J uses `TaskScheduler` to handle both dynamic one-time tasks (reminders) and static/dynamic recurring tasks (cron).

**Where in the code:** `SchedulerService`

```java
// One-time task execution at a specific Instant
taskScheduler.schedule(task, startTime);

// Recurring task execution using CronTrigger
taskScheduler.schedule(task, new CronTrigger(cronExpression));
```

### 2.2 Thread Management
By default, Spring Boot uses a single-threaded `TaskScheduler`. For an agent framework, we ensure it's configured for concurrency (via virtual threads in Java 25) so that long-running tasks don't block the heartbeat or other reminders.

---

## 3. Heartbeat & State Persistence

### 3.1 Why a Heartbeat?
The **Heartbeat Monitor** serves as the agent's "pulse". It runs every 15 minutes to:
1. Update `lastCheck` in `.memory/heartbeat-state.json`.
2. Provide a hook for future background tasks like memory compaction or RAG re-indexing.

### 3.2 File-based State
We use a JSON file (`heartbeat-state.json`) to persist state across application restarts. This ensures that even if the bot is redeployed, we have a record of when it was last active and what periodic checks were performed.

---

## 4. Time-Aware Agent Interactions

One of the highlights of Slice 6 is giving the agent **temporal awareness**.

### 4.1 Prompt Enrichment
In `AgentPlanner`, we now append the current system time and the user's specific context (User ID, Channel ID) to every system prompt.
- **Benefit:** The LLM knows *exactly* when "now" is, allowing it to calculate relative times like "remind me in 5 minutes".

### 4.2 Tool Logic
The `ReminderTool` bridges the gap between the LLM's intent and the `ReminderEngine`. It handles the parsing of ISO-8601 timestamps and cron patterns provided by the model.

---

## 5. Implementation Tips & Gotchas

### 5.1 Timezone Management
Always use `Instant` or `OffsetDateTime` (ISO-8601) when communicating with the LLM or storing times. Avoid `LocalDateTime` unless you are sure of the offset context, as it doesn't carry timezone information.

### 5.2 Tool Context
For the agent to set a reminder correctly, it needs to know *where* to send it. By including the `channelId` and `userId` in the system prompt, the LLM can populate the tool arguments accurately without "guessing".

---

## 6. Exercises

1.  **Dynamic Heartbeat:** Modify `HeartbeatMonitor` to perform a "health check" on the Vector Store and Slack API, logging the results to the state file.
2.  **Relative Reminders:** Test the agent with "Remind me in 2 minutes to take a break". Observe how the LLM calculates the absolute ISO-8601 timestamp based on the "Current Time" provided in the prompt.
3.  **Cron Wizard:** Try "Remind me every Monday at 9am to check the dashboard". Verify that the agent generates a valid cron expression and registers it with the `ReminderEngine`.
