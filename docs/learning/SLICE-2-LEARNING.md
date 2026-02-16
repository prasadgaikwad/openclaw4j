# Slice 2 — Learning Guide

> **What you built:** An intelligent agent that "thinks" using an LLM (OpenAI) instead of just echoing messages.
> **Concepts covered:** Spring AI `ChatClient`, LLM Prompt Engineering, In-Memory State Management, and Dependency Resolution strategies.

---

## 1. Spring AI Concepts

### 1.1 ChatClient

**What:** The primary interface for interacting with AI models. It provides a fluent API to build prompts, attach context, and execute requests.

**Where in the code:** `AgentPlanner`

```java
// Fluent API usage enables clean, readable chains
String response = chatClient.prompt()
    .system("You are a helpful assistant")
    .user("Hello World")
    .call()
    .content();
```

**Key rules:**
- It abstracts away the underlying provider (OpenAI, Ollama, Anthropic, etc.).
- You build a `Prompt` which contains a sequence of `Message` objects.
- `.call()` executes the request synchronously (blocking). Async/Stream support is available via `.stream()`.

### 1.2 Message Types

**What:** Spring AI defines standard message roles to structure the conversation context for the LLM.

**Where in the code:** `AgentPlanner`, `AgentService`

- **SystemMessage**: Instructions for the AI's behavior, persona, and constraints.
- **UserMessage**: The input from the human user.
- **AssistantMessage**: The AI's previous responses (used to build conversation history).

```java
List<Message> messages = new ArrayList<>();
messages.add(new SystemMessage(systemPrompt)); // "You are a pirate..."
messages.add(new UserMessage(userInput));      // "Hello there!"
```

**Why this matters:**
- Maintaining the distinction between roles helps the LLM understand who said what.
- System messages are "privileged" instructions that guide behavior more strongly than user messages.

---

## 2. Java & State Management

### 2.1 ConcurrentHashMap for Short-Term Memory

**What:** Using a thread-safe map to store conversation history in memory, keyed by channel or thread ID.

**Where in the code:** `ShortTermMemory`

```java
private final Map<String, List<Message>> history = new ConcurrentHashMap<>();
```

**Why this matters:**
- **Thread Safety:** Multiple users might message at the same time. `ConcurrentHashMap` allows concurrent reads and safe updates without explicit `synchronized` blocks.
- **Simplicity:** For an MVP, complex external databases (Redis/Postgres) introduce unnecessary overhead.
- **Volatility:** Data is lost on restart. This is acceptable for *Short-Term* working memory, but necessitates the *Long-Term* memory layers we will build in future slices.

---

## 3. Architecture & Patterns

### 3.1 Context Object Pattern

**What:** Encapsulating all necessary data for a decision into a single immutable record.

**Where in the code:** `AgentContext`

```java
public record AgentContext(
    InboundMessage message,
    List<Message> conversationHistory,
    MemorySnapshot memory,
    AgentProfile profile,
    // ...
) {}
```

**Why:**
- **Decoupling:** The `AgentPlanner` doesn't need to know *how* to fetch memory or profiles. It just receives a fully populated context.
- **Testability:** You can easily create a dummy `AgentContext` to test the planner's logic without mocking database calls or external services.
- **Statelessness:** The planner function remains pure (Context → Plan).

---

## 4. Troubleshooting & Ecosystem

### 4.1 Spring Boot vs. Spring AI Compatibility

**The Problem:**
We initially attempted to use **Spring Boot 4.0.2** with **Spring AI 1.1.2**.
- Spring AI currently relies on auto-configuration classes (like `RestClientAutoConfiguration`) that assume the Spring Boot 3.x ecosystem.
- This resulted in a `TypeNotPresentException` at startup because the 4.x structure differs.

**The Fix:**
- Downgraded the project to **Spring Boot 3.5.10**.
- This aligned the dependencies, allowing the auto-configuration to work correctly.

**Key Takeaway:**
Always check the "Reference Documentation" or "Compatability Matrix" when using experimental or rapidly evolving projects like Spring AI. Being on the "bleeding edge" (Boot 4.x) often breaks compatibility with ecosystem libraries.

---

## 5. Exercises

Try these to deepen your understanding:

1. **Change the Personality:** Modify `AgentService` to give the agent a specific persona (e.g., a skeptical detective) via the `SystemMessage` in the profile.
2. **Add Token Limits:** Update `ShortTermMemory` to evict messages based on total character count, rather than just message count, to fit within LLM context windows.
3.  **Try a Different Model:** Change `application.yml` to use `gpt-3.5-turbo` for speed/cost, or `gpt-4-turbo` for better reasoning.
4. **Log Token Usage:** Inspect the `ChatResponse` metadata in `AgentPlanner` to log how many tokens each request uses (useful for cost monitoring).
