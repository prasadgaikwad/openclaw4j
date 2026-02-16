# Slice 1 — Learning Guide

> **What you built:** A Spring Boot app that receives Slack messages and echoes them back through a clean agent pipeline.
> **Concepts covered:** Java 25 language features, Spring Boot 4.0.2 patterns, Slack Bolt SDK, and functional design.

---

## 1. Java 25 Features Used

### 1.1 Records

**What:** Records are immutable data carriers. The compiler generates `equals()`, `hashCode()`, `toString()`, and accessor methods automatically.

**Where in the code:** `InboundMessage`, `OutboundMessage`, `ChannelType.Slack`, `SlackProperties`

```java
// Before records (traditional Java):
public final class InboundMessage {
    private final String channelId;
    private final String userId;
    // ... constructor, getters, equals, hashCode, toString — 50+ lines of boilerplate

// With records (Java 25):
public record InboundMessage(
    String channelId,
    String userId,
    String content,
    ChannelType source,
    Instant timestamp,
    Map<String, String> metadata
) { }
// That's it. All methods are auto-generated.
```

**Key rules:**
- All fields are `final` and `private` — records are always immutable
- Accessor methods are named `fieldName()`, not `getFieldName()`
- You can add custom methods, but you cannot add mutable state
- Records can implement interfaces (like `ChannelType`)

**Further reading:** [JEP 395 — Records](https://openjdk.org/jeps/395)

---

### 1.2 Sealed Interfaces

**What:** A sealed interface restricts which classes can implement it. The compiler knows all possible subtypes, enabling exhaustive `switch` expressions without a `default` branch.

**Where in the code:** `ChannelType`, `ChannelAdapter`

```java
// The sealed keyword + permits clause restricts implementations
public sealed interface ChannelType {
    record Slack(String workspaceId) implements ChannelType {}
    record Discord(String guildId) implements ChannelType {}
    record WhatsApp(String phoneNumberId) implements ChannelType {}
}

// Exhaustive switch — compiler verifies all cases are handled
String label = switch (channelType) {
    case ChannelType.Slack s     -> "Slack: " + s.workspaceId();
    case ChannelType.Discord d   -> "Discord: " + d.guildId();
    case ChannelType.WhatsApp w  -> "WhatsApp: " + w.phoneNumberId();
    // No default needed! Compiler knows these are all the cases.
};
```

**Why this matters:**
- If you add a new channel type, every `switch` that doesn't handle it will fail to compile
- This prevents the classic bug of forgetting to handle a new case at runtime

**Further reading:** [JEP 409 — Sealed Classes](https://openjdk.org/jeps/409)

---

### 1.3 Compact Constructors

**What:** Records support a compact constructor — a constructor body *without* a parameter list that runs validation before field assignment.

**Where in the code:** `InboundMessage`, `OutboundMessage`

```java
public record InboundMessage(String channelId, String userId, ...) {

    // Compact constructor — no parameter list!
    // Runs before fields are assigned.
    public InboundMessage {
        if (channelId == null || channelId.isBlank()) {
            throw new IllegalArgumentException("channelId must not be null or blank");
        }
        // You can also transform parameters:
        if (metadata == null) {
            metadata = Map.of();  // Default to empty map
        }
    }
}
```

---

### 1.4 Text Blocks

**What:** Multi-line string literals delimited by `"""`. Supports indentation and string formatting.

**Where in the code:** `AgentService.echo()`

```java
var responseText = """
        🦞 **OpenClaw received your message:**
        > %s
        
        _I'm currently in echo mode (Slice 1)._\
        """.formatted(message.content());
```

**Key rules:**
- Opening `"""` must be followed by a newline
- Indentation is stripped based on the closing `"""`'s position
- Use `\` at end of line to suppress the newline (line continuation)
- `.formatted()` replaces `String.format()` — it's an instance method on String

---

### 1.5 Pattern Matching (Preview for Future Slices)

**What:** Java 25 enhances `switch` with pattern matching for `instanceof`, records, and even primitive types.

**Where we'll use it:** Routing by channel type, handling tool results (Slices 2–3)

```java
// Pattern matching with instanceof (already stable)
if (adapter instanceof SlackChannelAdapter slack) {
    slack.someSlackSpecificMethod();
}

// Pattern matching in switch with record deconstruction (Slice 2+)
switch (channelType) {
    case ChannelType.Slack(var workspaceId) -> 
        log.info("Handling Slack workspace: {}", workspaceId);
    case ChannelType.Discord(var guildId) -> 
        log.info("Handling Discord guild: {}", guildId);
    case ChannelType.WhatsApp(var phoneId) -> 
        log.info("Handling WhatsApp: {}", phoneId);
}
```

---

## 2. Spring Boot 4.0.2 Concepts

### 2.1 @ConfigurationProperties with Records

**What:** Spring Boot can bind YAML properties directly to record constructors — no `@Value` annotations, no setter methods.

**Where in the code:** `SlackProperties`

```java
@ConfigurationProperties(prefix = "openclaw4j.channel.slack")
public record SlackProperties(
    String botToken,       // bound from: openclaw4j.channel.slack.bot-token
    String signingSecret   // bound from: openclaw4j.channel.slack.signing-secret
) {}
```

**How binding works:**
```yaml
openclaw4j:
  channel:
    slack:
      bot-token: ${SLACK_BOT_TOKEN:}        # → botToken
      signing-secret: ${SLACK_SIGNING_SECRET:}  # → signingSecret
```

Spring's **relaxed binding** converts `bot-token` (kebab-case) → `botToken` (camelCase).

**Activation:** `@EnableConfigurationProperties(SlackProperties.class)` on a `@Configuration` class.

---

### 2.2 Auto-Configuration & Starters

**What:** Spring Boot starters are curated dependency bundles that auto-configure components.

**Starters used in Slice 1:**

| Starter | What It Auto-Configures |
|---------|------------------------|
| `spring-boot-starter-web` | Embedded Tomcat, Spring MVC, Jackson JSON |
| `spring-boot-starter-actuator` | Health checks, metrics, info endpoints |
| `spring-boot-starter-test` | JUnit 5, Mockito, Spring Test, AssertJ |

**Spring Boot 4.0.2 improvement:** Starters are now more granular (modular). Instead of one huge `spring-boot-autoconfigure` JAR, each module has its own auto-configuration. This means faster startup and smaller dependency trees.

---

### 2.3 ServletRegistrationBean

**What:** Registers a non-Spring servlet in Spring Boot's embedded Tomcat.

**Where in the code:** `SlackAppConfig.slackAppServlet()`

```java
@Bean
public ServletRegistrationBean<SlackAppServlet> slackAppServlet(App app) {
    return new ServletRegistrationBean<>(new SlackAppServlet(app), "/slack/events");
}
```

**Why?** The Slack Bolt SDK has its own servlet (`SlackAppServlet`), which doesn't use Spring MVC. `ServletRegistrationBean` lets us run it alongside Spring MVC's `DispatcherServlet` on the same Tomcat instance.

---

### 2.4 Spring Boot Actuator

**What:** Production-ready monitoring and management endpoints.

**Configured endpoint:**
- `GET /actuator/health` → `{"status": "UP"}` when the app is running

In future slices, we'll add custom health indicators (e.g., "is the LLM reachable?", "is the vector store healthy?").

---

## 3. Slack Bolt SDK Concepts

### 3.1 Architecture

```
Slack Platform
    │
    ▼ HTTP POST (event payload)
SlackAppServlet (/slack/events)
    │
    ▼ Signature verification (using signing secret)
Bolt App (middleware chain)
    │
    ▼ Event dispatching
Your event handler
    │
    ▼ ctx.ack() — must respond within 3 seconds
Slack Platform (acknowledges receipt)
```

### 3.2 Key Components

| Component | Purpose |
|-----------|---------|
| `App` | The Bolt application — holds all event/command handlers |
| `AppConfig` | Configuration (bot token, signing secret) |
| `SlackAppServlet` | Adapts Bolt to the Servlet API |
| `ctx.ack()` | Acknowledges the event to Slack (required within 3 seconds) |
| `MethodsClient` | Imperative client for Slack Web API calls (`chat.postMessage`, etc.) |

### 3.3 Event Types

In Slice 1, we handle:
- **`MessageEvent`** — fired when a message is posted in a channel where the bot is present

In future slices, we may handle:
- `AppMentionEvent` — when someone @mentions the bot
- `ReactionAddedEvent` — when a reaction emoji is added
- Slash commands — like `/openclaw do something`

### 3.4 Bot Loop Prevention

```java
if (event.getSubtype() != null) {
    return ctx.ack();  // Ignore bot messages, edits, etc.
}
```

Without this check, the bot would respond to its own messages, creating an infinite loop.

---

## 4. Design Patterns Used

### 4.1 Adapter Pattern

**Problem:** Slack, Discord, and WhatsApp all have different APIs and event formats.

**Solution:** Each channel has an *adapter* that translates between the platform-specific format and our normalized domain types (`InboundMessage`, `OutboundMessage`).

```
Slack Event → SlackChannelAdapter → InboundMessage → AgentService
                                                          │
                                     OutboundMessage ←────┘
                                          │
SlackChannelAdapter → Slack chat.postMessage API
```

The agent core **never touches** any platform API directly.

### 4.2 Function Pipeline

**Problem:** The agent's processing logic will grow complex across slices.

**Solution:** Express processing as a chain of `Function<A, B>` steps that can be composed.

```java
// Slice 1: Simple echo
this.processingPipeline = this::echo;

// Slice 4+: Composed pipeline
this.processingPipeline = enrichWithMemory
    .andThen(enrichWithRAG)
    .andThen(planWithLLM)
    .andThen(executeTools)
    .andThen(composeResponse);
```

### 4.3 Immutable Domain Model

All domain types (`InboundMessage`, `OutboundMessage`, `ChannelType`) are records — immutable by design. This means:
- **Thread safety for free** — no synchronization needed
- **Easier reasoning** — a message object never changes after creation
- **Testing simplicity** — create input, assert output, done

---

## 5. Project Structure Rationale

```
src/main/java/dev/prasadgaikwad/openclaw4j/
├── OpenClaw4jApplication.java    # Entry point — one class, one job
├── agent/                        # Agent core — processing logic
│   └── AgentService.java
└── channel/                      # Channel abstraction layer
    ├── ChannelAdapter.java       # Sealed interface — the contract
    ├── ChannelType.java          # Sealed interface — the identity
    ├── InboundMessage.java       # What comes in (normalized)
    ├── OutboundMessage.java      # What goes out (normalized)
    └── slack/                    # Slack-specific implementation
        ├── SlackAppConfig.java
        ├── SlackChannelAdapter.java
        └── SlackProperties.java
```

**Why this structure?**
- `channel/` is the abstraction layer — it defines the contract
- `channel/slack/` is one implementation — it knows about Slack
- `agent/` is the core — it only knows about normalized types
- Adding Discord = new `channel/discord/` folder, no changes to `agent/`

---

## 6. Exercises

Try these to deepen your understanding:

1. **Add a `/openclaw` slash command** — register it in `SlackAppConfig` and have the agent respond
2. **Add an `@mention` handler** — respond only when the bot is explicitly mentioned
3. **Add a `ChannelType.Console`** — create a `ConsoleChannelAdapter` that reads from stdin and prints to stdout (great for local testing without Slack)
4. **Write a test for `OutboundMessage.textReply()`** — verify the factory method creates correct defaults
5. **Try exhaustive switch** — write a method that switches on `ChannelType` and see what happens when you add a new record to the sealed interface
