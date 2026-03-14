# Learning: Empty LLM Response After Tool Execution

> **Problem:** After `ToolCallAdvisor` completes the ReAct loop (tool call → result → LLM), the model sometimes returns empty content. The user gets a blank or generic "I fetched results" reply.
> **Concepts covered:** ReAct loop mechanics, Spring AI `ToolCallAdvisor`, LLM turn completion behaviour, ThreadLocal context propagation, Spring AOP for cross-cutting concerns.

---

## 1. Why Does This Happen?

### The ReAct Loop (Reason + Act)

OpenClaw4J uses Spring AI's `ToolCallAdvisor` to run the full ReAct loop automatically inside a single `ChatClient.prompt().call()`:

```
User message
   → LLM decides to call a tool (emits tool_call JSON, no text content)
   → ToolCallAdvisor executes the tool
   → Tool result is appended to the conversation as a ToolResponseMessage
   → LLM is called again with the tool result
   → LLM should now synthesise a final text answer
```

### Where It Goes Wrong

The problem is in **step 6**. Most LLMs handle it correctly, but under specific conditions the model completes its "turn" after receiving the tool result and emits an **empty `content` field** instead of a synthesis. This is not a Spring AI bug — it is a known LLM behaviour that occurs when:

| Trigger | Why |
|---------|-----|
| **Long tool output** | The model processes a large tool result, treats it as context-absorbed, and stops generating — it feels "done". |
| **Model temperature / randomness** | Non-deterministic sampling occasionally produces zero tokens. |
| **Prompt structure** | If the system prompt does not explicitly mandate a final user-facing response, the model may stop after internal reasoning. |
| **Tool result that reads like a final answer** | If the tool already returns a structured summary, the model can infer the work is done. |

### What Spring AI Returns

`ChatClient.prompt().call().content()` returns `null` or `""` when the LLM finishes the turn without generating any `content` tokens (even though it successfully executed the tool).

---

## 2. How OpenClaw4J Mitigates It

The fix uses three layers of defence.

### Layer 1 — System Prompt Mandate

`AgentPlanner.coreInstructions` contains an explicit rule:

```
4. FINAL SYNTHESIS (CRITICAL): Once you have enough information, you MUST
   provide a final, helpful, and concise answer to the user.
   - NEVER end your response with just tool JSON or empty content.
   - If you used search results, summarize them in your own words.
```

This reduces the frequency of the issue but is **not sufficient on its own** — LLMs do not 100% respect system instructions.

### Layer 2 — Context-Aware Recovery Re-Prompt

When the primary response is empty, `AgentPlanner.fireRecoveryPrompt()` fires a second, lightweight LLM call that **includes the actual tool output** in its system prompt:

```java
if (toolResults != null && !toolResults.isBlank()) {
    recoverySystemPromptBuilder
        .append("\n\n### Tool Results:\n")
        .append(toolResults)
        .append("\n\nUsing only the information above, answer the user's question. "
              + "Be specific, informative, and concise.");
}
```

**Before this fix:** Recovery prompt had no tool context → generic "I fetched results for you."
**After this fix:** Recovery prompt has full tool output → real, accurate synthesis.

### Layer 3 — `ToolResultStore` + AOP Aspect

To capture tool results **for every tool** without modifying each one:

**`ToolResultStore`** — a `ThreadLocal`-backed store (same pattern as `ReminderContext`):

```java
public class ToolResultStore {
    private static final ThreadLocal<String> store = new ThreadLocal<>();
    public static void set(String result) { store.set(result); }
    public static String get() { return store.get(); }
    public static void clear() { store.remove(); }
}
```

**`ToolResultStoreAspect`** — an AOP `@Around` advice that intercepts every `@Tool`-annotated method on any `AITool` bean:

```java
@Around("within(dev.prasadgaikwad.openclaw4j.tool.AITool+) " +
        "&& @annotation(org.springframework.ai.tool.annotation.Tool)")
public Object captureToolResult(ProceedingJoinPoint pjp) throws Throwable {
    Object result = pjp.proceed();
    if (result instanceof String toolOutput && !toolOutput.isBlank()) {
        ToolResultStore.set(toolOutput);
    }
    return result;
}
```

The store is cleared in `AgentService`'s `finally` block to prevent thread-local leaks in the thread pool.

---

## 3. The Full Request Lifecycle (After Fix)

```
AgentService.process()
  └── ToolResultStore.clear() is ready (not yet set)
  └── AgentPlanner.plan()
        └── chatClient.prompt()
              .advisors(ToolCallAdvisor)   ← ReAct loop
              .tools(...)
              .call()
              │
              ├── LLM calls SearchTool.search()
              │     └── ToolResultStoreAspect intercepts → ToolResultStore.set(results)
              │
              ├── LLM receives tool result
              └── LLM synthesises (or returns empty)

  If empty → fireRecoveryPrompt(userInput, ToolResultStore.get())
               └── Recovery LLM call with full tool output in system prompt
               └── Returns real synthesis ✅

  Finally → ToolResultStore.clear()
           → ReminderContext.clear()
```

---

## 4. Java Concepts Used

### `ThreadLocal<T>`

A `ThreadLocal` provides a per-thread variable — each thread has its own independent instance. This is ideal for request-scoped, stateful data in a thread-pool environment (like Spring's embedded Tomcat).

```java
ThreadLocal<String> store = new ThreadLocal<>();
store.set("data");    // stored for THIS thread only
store.get();          // "data" — only on this thread
store.remove();       // IMPORTANT: always clean up to prevent leaks
```

**Key rule:** Always `remove()` in a `finally` block. If the thread is returned to the pool with stale data, the next request on that thread will see it.

### Spring AOP — `@Around` Advice

An `@Around` advice wraps a method call — it can inspect, modify, or replace both the arguments and return value:

```java
@Around("pointcut expression")
public Object advice(ProceedingJoinPoint pjp) throws Throwable {
    // Before
    Object result = pjp.proceed();  // Execute the actual method
    // After
    return result;
}
```

The pointcut `within(AITool+)` matches any class that implements `AITool` or a subtype of it. Combined with `@annotation(Tool)`, it precisely targets only methods the LLM can invoke.

---

## 5. Other Approaches Considered

### A. Manual `set()` in Each Tool ❌ (Rejected)
Calling `ToolResultStore.set()` directly inside each tool method works but violates DRY — every new tool must remember to do it. AOP is the right abstraction.

### B. Custom `ToolCallAdvisor` / Spring AI `ToolExecutionListener` 🔄 (Future)
Spring AI's advisor chain allows composing custom advisors alongside `ToolCallAdvisor`. A custom advisor could intercept `ToolResponseMessage` objects and capture results more precisely. This would work for MCP tools too (which the AOP aspect cannot intercept). Worth exploring when Spring AI stabilises its advisor SPI.

### C. Spring AI `tool_choice: "required"` ⚠️ (Partial)
Setting `tool_choice: "required"` at the model level forces the LLM to always call a tool. While useful for specific scenarios, it causes infinite loops if applied across the full ReAct cycle (every turn would be forced to call a tool again). Not applicable as a general fix.

### D. Streaming Mode 🔄 (Alternative Direction)
In streaming mode, the model emits tokens incrementally. Some reports suggest streaming reduces empty-response frequency because the model commits to generating tokens earlier. However, Spring AI streaming has had its own content-swallowing bugs ([spring-ai#2575](https://github.com/spring-projects/spring-ai/issues/2575)). Not yet recommended.

### E. Prompt Engineering — "Think Out Loud" (Partial)
Instructing the model to use chain-of-thought reasoning before answering (e.g., "Think step-by-step before answering") can reduce silent completions. However, it increases token consumption and latency. The system prompt mandate in `coreInstructions` is a lightweight version of this.

### F. Spring AI Upstream Fix
This issue has been acknowledged in the Spring AI ecosystem. A proper fix would be for `ToolCallAdvisor` to detect an empty final content response and either:
1. Retry the synthesis step automatically.
2. Return a structured error that callers can handle.

There is no official fix as of Spring AI `1.1.2`. Tracking: check [Spring AI GitHub issues](https://github.com/spring-projects/spring-ai/issues) for `ToolCallAdvisor empty response`.

---

## 6. Gotchas

### 6.1 MCP Tools Are Not Covered by the AOP Aspect
The `@Around` advice only intercepts local `AITool` beans. MCP tools are `ToolCallback` objects invoked by Spring AI internally — their execution does not pass through the Spring AOP proxy. If a recovery is needed after an MCP tool call, `ToolResultStore` will be null. The fallback message handles this gracefully.

### 6.2 Multiple Tool Calls in One Turn
If the LLM calls two tools sequentially in one ReAct loop, `ToolResultStore` holds only the **last** result (each `set()` overwrites). For most queries this is fine — the recovery prompt only needs context about the most recent (usually the substantive) tool. A future enhancement could accumulate results with `append()`.

### 6.3 Tool Errors Are Also Captured
If a tool returns an error string (e.g., `"Error: Tavily API key not configured"`), the aspect stores that too. The recovery prompt will include the error in its context — which is actually useful, since the model can relay the error to the user rather than guessing.

### 6.4 Thread Safety
`ThreadLocal` is inherently thread-safe — each thread has its own copy. No synchronisation is needed. The only risk is forgetting to `clear()`, which is handled in `AgentService.finally`.

---

## 7. Exercises

1. **MCP Tool Coverage:** Extend the recovery mechanism to handle MCP tools. One approach: write a custom `RequestResponseAdvisor` that inspects `ToolResponseMessage` objects in the conversation history and extracts their content into `ToolResultStore`.
2. **Accumulate Multiple Results:** Change `ToolResultStore` to accumulate results from multiple tool calls (e.g., a `List<String>` or newline-separated `StringBuilder`) and pass all of them to the recovery prompt.
3. **Metrics:** Add a Micrometer counter that increments every time the recovery prompt fires. Expose it via `/actuator/metrics`. Use this to track how often the primary LLM response fails per day.
4. **Recovery Unit Test:** Write a test for `AgentPlannerTest` that simulates two sequential tool calls (set `ToolResultStore` twice) and verifies only the last result is passed to the recovery prompt.
5. **Streaming Experiment:** Write a branch that uses Spring AI's streaming (`stream().content()`) instead of `call().content()`. Compare the empty-response rate under load.
