# Slice 8 — Learning Guide: WhatsApp Channel

> **What you built:** A full WhatsApp channel adapter integrating the WhatsApp Business Cloud API (Meta) using Spring's `RestClient` and a standard `@RestController` webhook.
> **Concepts covered:** WhatsApp Cloud API, Webhook verification, Spring `RestClient`, REST-based channel adapters, and extending sealed type hierarchies.

---

## 1. Conceptual Model: WhatsApp Integration

| Feature | Implementation | Purpose |
|---------|----------------|---------|
| **Outbound Messages** | `WhatsAppChannelAdapter` + `RestClient` | Sends agent responses to users via the WhatsApp Cloud API. |
| **Inbound Webhook** | `WhatsAppWebhookController` | Receives user messages from Meta's webhook system and normalizes them to `InboundMessage`. |
| **Configuration** | `WhatsAppProperties` + `WhatsAppConfig` | Binds environment variables (access token, phone number ID, verify token) and creates a pre-configured `RestClient`. |
| **Async Processing** | `ExecutorService` in Controller | Acknowledges webhooks immediately (< 5s) while processing the LLM call in the background. |

---

## 2. WhatsApp Cloud API vs Slack Bolt SDK

One of the key architectural decisions in this slice was **not** using an external SDK.

| Aspect | Slack | WhatsApp |
|--------|-------|----------|
| **SDK** | Bolt SDK (`com.slack.api:bolt`) | None — REST API only |
| **Inbound** | Bolt servlet (`SlackAppServlet`) | Standard `@RestController` |
| **Outbound** | `MethodsClient.chatPostMessage()` | `RestClient.post()` to Graph API |
| **Auth** | Bot Token + Signing Secret | Bearer Token + Verify Token |
| **Event Format** | Bolt-managed event types | Raw JSON webhook payloads |
| **Dependencies** | 2 JARs (bolt + bolt-jakarta-servlet) | 0 additional JARs |

**Why no SDK?** The WhatsApp Cloud API is a straightforward REST API. Using Spring's built-in `RestClient` keeps the dependency footprint minimal and leverages Spring's HTTP error handling, interceptors, and observability — all things we'd lose with an external SDK.

---

## 3. Webhook Verification — The Handshake

Before Meta sends you any messages, it needs to verify that you own the webhook URL. This is a one-time handshake.

**Where in the code:** `WhatsAppWebhookController.verifyWebhook()`

```java
@GetMapping
public ResponseEntity<String> verifyWebhook(
        @RequestParam("hub.mode") String mode,
        @RequestParam("hub.verify_token") String token,
        @RequestParam("hub.challenge") String challenge) {
    
    if ("subscribe".equals(mode) && properties.verifyToken().equals(token)) {
        return ResponseEntity.ok(challenge);  // Echo the challenge back
    }
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
}
```

**How it works:**
1. You register your webhook URL (e.g., `https://your-app.com/whatsapp/webhook`) in the Meta Developer Dashboard.
2. Meta sends a GET request with `hub.mode=subscribe`, your `hub.verify_token`, and a random `hub.challenge`.
3. If the token matches your config, you respond with the challenge as plain text.
4. Meta considers the webhook verified and starts sending events.

---

## 4. Spring RestClient — Modern HTTP Client

Spring Framework 6.1 introduced `RestClient` as a modern, fluent replacement for `RestTemplate`.

**Where in the code:** `WhatsAppConfig.whatsappRestClient()`

```java
@Bean("whatsappRestClient")
public RestClient whatsappRestClient(WhatsAppProperties properties) {
    return RestClient.builder()
            .baseUrl("https://graph.facebook.com/" + properties.apiVersion())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
}
```

**Key advantages over `RestTemplate`:**
- **Fluent API**: Method chaining (`post().uri().body().retrieve()`) reads like a sentence.
- **Builder pattern**: Pre-configure base URL, default headers, and interceptors once — every request inherits them.
- **Named beans**: Using `@Bean("whatsappRestClient")` and `@Qualifier` ensures the right client is injected.
- **Testability**: `MockRestServiceServer.bindTo(builder)` makes testing straightforward.

---

## 5. Sending Messages — Cloud API Format

**Where in the code:** `WhatsAppChannelAdapter.sendMessage()`

```java
var payload = Map.of(
    "messaging_product", "whatsapp",
    "recipient_type", "individual",
    "to", message.channelId(),           // recipient's phone number
    "type", "text",
    "text", Map.of("body", message.content()));

restClient.post()
    .uri("/{phoneNumberId}/messages", properties.phoneNumberId())
    .body(payload)
    .retrieve()
    .toEntity(String.class);
```

**Key points:**
- `messaging_product` must always be `"whatsapp"` — this is a Graph API convention.
- `to` is the recipient's phone number with country code (e.g., `"1234567890"`), without the `+` prefix.
- The URL path includes the **phone number ID** (not the phone number itself) — this identifies *your* business number.

---

## 6. Processing Inbound Messages — Payload Navigation

WhatsApp's webhook payload is deeply nested:

```
payload → entry[] → changes[] → value → messages[]
```

**Where in the code:** `WhatsAppWebhookController.processPayload()`

We navigate each level carefully, checking for null/empty at every step. This is because Meta sends different event types through the same webhook — status updates, read receipts, and actual messages all arrive here.

```java
var entries = (List<Map<String, Object>>) payload.get("entry");
for (var entry : entries) {
    var changes = (List<Map<String, Object>>) entry.get("changes");
    for (var change : changes) {
        var value = (Map<String, Object>) change.get("value");
        var messages = (List<Map<String, Object>>) value.get("messages");
        // Only process if messages exist (skip status updates)
        if (messages != null) {
            for (var msg : messages) {
                processMessage(msg, phoneNumberId);
            }
        }
    }
}
```

---

## 7. Extending the Channel Adapter Pattern

Adding WhatsApp followed the same pattern established in Slice 1:

```
channel/whatsapp/
├── WhatsAppProperties.java       ← Config binding (like SlackProperties)
├── WhatsAppConfig.java           ← API client setup (like SlackAppConfig)
├── WhatsAppChannelAdapter.java   ← Implements ChannelAdapter (like SlackChannelAdapter)
└── WhatsAppWebhookController.java ← Inbound events (replaces Bolt's servlet)
```

**Sealed type hierarchy preserved:** The `ChannelType.WhatsApp` record was already defined in the sealed interface from Slice 1. This means every `switch` expression over `ChannelType` was compiler-checked — the WhatsApp case was already handled.

---

## 8. Testing with MockRestServiceServer

**Where in the code:** `WhatsAppChannelAdapterTest`

Unlike Slack (where we mock the `MethodsClient`), with `RestClient` we use `MockRestServiceServer`:

```java
var restClientBuilder = RestClient.builder()
        .baseUrl("https://graph.facebook.com/v21.0");

mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
var restClient = restClientBuilder.build();

// In tests...
mockServer.expect(requestTo(".../123456789/messages"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("{...}", MediaType.APPLICATION_JSON));

adapter.sendMessage(outbound);
mockServer.verify();
```

This pattern intercepts outbound HTTP requests and verifies they match expected URLs, methods, and content types — all without hitting any real API.

---

## 9. Implementation Tips & Gotchas

### 9.1 Message Deduplication
WhatsApp retries webhook delivery if you don't respond with 200 OK within ~5 seconds. The `ConcurrentHashMap.newKeySet()` pattern prevents processing the same message twice — identical to the Slack dedup approach.

### 9.2 Phone Number Format
WhatsApp uses phone numbers **without** the `+` prefix. `"1234567890"` is valid; `"+1234567890"` will cause API errors.

### 9.3 Access Token Expiration
The temporary access token from Meta's testing tools expires in 24 hours. For production, you need a **System User Token** (permanent) from Meta Business Suite.

### 9.4 Webhook URL Must Be HTTPS
Meta requires HTTPS for webhooks. Use `ngrok` for local development: `ngrok http 8080`.

---

## 10. Exercises

1.  **Media Messages:** Extend `WhatsAppWebhookController.processMessage()` to handle `image` type messages. Log the image URL and respond with "I received your image!"
2.  **Message Templates:** WhatsApp requires pre-approved templates for initiating conversations. Add a `sendTemplate()` method to `WhatsAppChannelAdapter` that sends a template message.
3.  **Read Receipts:** Add handling for the `statuses` array in the webhook payload. Log when messages are delivered and read.
4.  **Rate Limiting:** The WhatsApp Cloud API has rate limits. Add a rate limiter using Spring's `@RateLimiter` or a simple token bucket to the `WhatsAppChannelAdapter`.
5.  **Integration Test:** Write an integration test using WireMock to simulate the full WhatsApp webhook → agent → response flow.
