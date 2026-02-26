# Slice 7 — Learning Guide: Polish & Expand

> **What you built:** Enhanced the agent's resilience with Spring Retry, improved compound task planning, and built a more observable heartbeat system with Spring Events.
> **Concepts covered:** Spring Retry (`@Retryable`), Multi-step ReAct planning, Application Events (`ApplicationEventPublisher`), and structured error handling.

---

## 1. Conceptual Model: Resilience & Planning

| Feature | Implementation | Purpose |
|-------|----------------|---------|
| **Resilient Core** | `AgentPlanner` + `@Retryable` | Automatically retries the planning loop if it fails due to transient LLM or tool issues. |
| **Compound Tasks** | System Prompt Update | Explicitly guides the LLM to break down complex requests and use tool outputs for subsequent steps. |
| **Advanced Heartbeat** | `HeartbeatMonitor` + `HeartbeatEvent` | Emits internal events that other components can listen to, providing better background observability. |
| **Error Feedback** | `AgentService` Try-Catch | Ensures the user receives a friendly explanation when a failure cannot be automatically recovered. |

---

## 2. Resilience with Spring Retry

In an agentic system, tool calls or LLM prompts can fail due to network blips, rate limits, or unexpected payload shapes.

**Where in the code:** `AgentPlanner.java`

```java
@Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000))
public String plan(AgentContext context) {
    // Complex planning logic here
}
```

- **Benefit:** Transient errors are handled transparently without the user ever seeing a failure message.
- **Requirement:** Must include `spring-retry` and `@EnableRetry` in the application configuration.

---

## 3. Guiding the Agent for Compound Tasks

By default, LLMs might try to do everything in one step. We've enhanced the **System Prompt** to enforce a structured "think-then-act" approach for complex workflows.

**Prompt Snippet used in Slice 7:**
> 1. ANALYZE the user request carefully.
> 2. BREAK DOWN complex requests into logical steps.
> 3. EXECUTE tools sequentially...
> 4. SUMMARIZE...

This "Planning Directive" makes the agent much more reliable when tasked with something like "Read these 10 messages, summarize them, and create a GitHub issue".

---

## 4. Building an Event-Driven Heartbeat

Instead of just updating a file, the heartbeat now acts as a **central heartbeat event provider** for the whole application.

### 4.1 The Heartbeat Event
We created a `record HeartbeatEvent` to carry the status map and timestamp.

### 4.2 ApplicationEventPublisher
By injecting `ApplicationEventPublisher` into `HeartbeatMonitor`, we can broadcast the system's pulse.

```java
eventPublisher.publishEvent(new HeartbeatEvent(now, heartbeat));
```

- **Future-proofing:** New modules (like a "Memory Compactor" or "Slack Cleanup Service") can simply listen for this event using `@EventListener` instead of setting up their own `@Scheduled` timers.

---

## 5. Implementation Tips & Gotchas

### 5.1 Retry Scope
Be careful not to annotate methods that have non-idempotent side effects with `@Retryable` unless you handle the idempotency inside the method. In our case, `AgentPlanner.plan` is safe because it's the high-level orchestrator of the ReAct cycle.

### 5.2 User Experience (UX)
Graceful degradation is key. If the planning fails even after multiple retries, `AgentService` catches the exception and sends a friendly "I encountered an error" message, preserving the user's trust.

---

## 6. Exercises

1.  **Custom Retry Logic:** Modify the `@Retryable` annotation to only retry on specific exceptions (e.g., `RestClientException`) and verify it doesn't retry on user errors (like `IllegalArgumentException`).
2.  **Heartbeat Listener:** Create a new component that uses `@EventListener` to listen for `HeartbeatEvent` and prints "System Pulse Received" every 15 minutes.
3.  **Complex Flow Simulation:** Ask the agent a three-part task: "Find my last message, tell me what time it was, and then remind me about it in 1 hour." Verify it uses the RAG tool/context, then the clock, and finally the Reminder tool.
