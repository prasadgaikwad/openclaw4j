package dev.prasadgaikwad.openclaw4j.channel.whatsapp;

import dev.prasadgaikwad.openclaw4j.agent.AgentService;
import dev.prasadgaikwad.openclaw4j.channel.ChannelType;
import dev.prasadgaikwad.openclaw4j.channel.InboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST controller that handles inbound WhatsApp webhook events.
 *
 * <p>
 * The WhatsApp Cloud API uses a standard <b>HTTP webhook</b> pattern:
 * </p>
 * <ol>
 * <li><b>Verification (GET):</b> When you register the webhook URL in the Meta
 * Developer Dashboard, Meta sends a GET request with a challenge token.
 * You must echo it back to prove ownership.</li>
 * <li><b>Notifications (POST):</b> When a user sends a message, Meta posts
 * a JSON payload to this endpoint. We normalize it to an
 * {@link InboundMessage} and delegate to {@link AgentService}.</li>
 * </ol>
 *
 * <h3>Comparison with Slack's Approach</h3>
 * <p>
 * Slack uses the Bolt SDK which registers its own servlet
 * ({@code SlackAppServlet} at {@code /slack/events}). WhatsApp's Cloud API
 * is simpler — it's just REST, so we use a standard Spring
 * {@code @RestController}. This is idiomatic Spring and does not require
 * any external SDK.
 * </p>
 *
 * <h3>Webhook Payload Structure (Simplified)</h3>
 * 
 * <pre>
 * {
 *   "object": "whatsapp_business_account",
 *   "entry": [{
 *     "id": "BUSINESS_ACCOUNT_ID",
 *     "changes": [{
 *       "value": {
 *         "messaging_product": "whatsapp",
 *         "metadata": { "phone_number_id": "...", "display_phone_number": "..." },
 *         "contacts": [{ "wa_id": "1234567890", "profile": { "name": "User" } }],
 *         "messages": [{
 *           "id": "wamid.xxx",
 *           "from": "1234567890",
 *           "timestamp": "1234567890",
 *           "type": "text",
 *           "text": { "body": "Hello!" }
 *         }]
 *       },
 *       "field": "messages"
 *     }]
 *   }]
 * }
 * </pre>
 *
 * @author Prasad Gaikwad
 * @see WhatsAppChannelAdapter
 * @see WhatsAppConfig
 */
@RestController
@RequestMapping("/whatsapp/webhook")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final AgentService agentService;
    private final WhatsAppChannelAdapter channelAdapter;
    private final WhatsAppProperties properties;

    // Deduplication set to prevent processing the same message twice.
    // In production, use a cache with TTL (e.g., Caffeine or Redis).
    private final Set<String> processedMessages = ConcurrentHashMap.newKeySet();

    // Executor for processing agent tasks asynchronously.
    // WhatsApp expects a 200 OK within 5 seconds or it will retry the webhook.
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public WhatsAppWebhookController(
            AgentService agentService,
            WhatsAppChannelAdapter channelAdapter,
            WhatsAppProperties properties) {
        this.agentService = agentService;
        this.channelAdapter = channelAdapter;
        this.properties = properties;
    }

    /**
     * Handles the webhook verification challenge from Meta.
     *
     * <h3>Webhook Verification Protocol</h3>
     * <p>
     * When you configure the webhook URL in the Meta Developer Dashboard,
     * Meta sends a GET request with these query parameters:
     * </p>
     * <ul>
     * <li>{@code hub.mode} — should be "subscribe"</li>
     * <li>{@code hub.verify_token} — must match your configured verify token</li>
     * <li>{@code hub.challenge} — the value you must echo back</li>
     * </ul>
     * <p>
     * If the verify token matches, respond with the challenge as plain text.
     * Otherwise, respond with 403 Forbidden.
     * </p>
     *
     * @param mode      the hub mode (should be "subscribe")
     * @param token     the verification token to validate
     * @param challenge the challenge string to echo back
     * @return the challenge string if verified, or 403 if not
     */
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        log.info("WhatsApp webhook verification request: mode={}", mode);

        if ("subscribe".equals(mode) && properties.verifyToken().equals(token)) {
            log.info("WhatsApp webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }

        log.warn("WhatsApp webhook verification failed: invalid token");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    /**
     * Handles incoming WhatsApp message notifications.
     *
     * <h3>Processing Flow</h3>
     * <ol>
     * <li>Acknowledge immediately with 200 OK (WhatsApp retries after 5s)</li>
     * <li>Extract message data from the nested JSON structure</li>
     * <li>Deduplicate using the WhatsApp message ID</li>
     * <li>Normalize to {@link InboundMessage}</li>
     * <li>Delegate to {@link AgentService} asynchronously</li>
     * <li>Send response via {@link WhatsAppChannelAdapter}</li>
     * </ol>
     *
     * <h3>Why Async?</h3>
     * <p>
     * WhatsApp Cloud API expects a 200 OK response quickly. If the response
     * takes too long, Meta will retry the webhook delivery, potentially causing
     * duplicate processing. By processing asynchronously and responding
     * immediately, we avoid this — the same pattern used in
     * {@link dev.prasadgaikwad.openclaw4j.channel.slack.SlackAppConfig}.
     * </p>
     *
     * @param payload the raw webhook payload from Meta
     * @return 200 OK immediately
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload) {
        log.debug("Received WhatsApp webhook payload: {}", payload);

        // Respond immediately — WhatsApp requires a quick 200 OK.
        // Process the message asynchronously.
        executor.submit(() -> processPayload(payload));

        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    /**
     * Processes the webhook payload asynchronously.
     *
     * <h3>WhatsApp Payload Navigation</h3>
     * <p>
     * The WhatsApp Cloud API uses a deeply nested JSON structure:
     * {@code payload.entry[].changes[].value.messages[]}. We navigate
     * each level carefully, checking for null/empty at every step to
     * handle status updates and other non-message events gracefully.
     * </p>
     */
    @SuppressWarnings("unchecked")
    private void processPayload(Map<String, Object> payload) {
        try {
            // Navigate the nested structure: entry → changes → value → messages
            var entries = (List<Map<String, Object>>) payload.get("entry");
            if (entries == null || entries.isEmpty())
                return;

            for (var entry : entries) {
                var changes = (List<Map<String, Object>>) entry.get("changes");
                if (changes == null)
                    continue;

                for (var change : changes) {
                    var value = (Map<String, Object>) change.get("value");
                    if (value == null)
                        continue;

                    var messages = (List<Map<String, Object>>) value.get("messages");
                    if (messages == null || messages.isEmpty())
                        continue;

                    // Extract metadata for the phone number ID
                    var metadata = (Map<String, Object>) value.get("metadata");
                    var phoneNumberId = metadata != null
                            ? String.valueOf(metadata.get("phone_number_id"))
                            : properties.phoneNumberId();

                    for (var msg : messages) {
                        processMessage(msg, phoneNumberId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing WhatsApp webhook payload", e);
        }
    }

    /**
     * Processes a single WhatsApp message.
     *
     * <p>
     * Currently supports {@code text} message types. Other types (image,
     * audio, location, etc.) are logged and skipped. Future slices can
     * extend this with media handling.
     * </p>
     */
    @SuppressWarnings("unchecked")
    private void processMessage(Map<String, Object> msg, String phoneNumberId) {
        var messageId = String.valueOf(msg.get("id"));
        var from = String.valueOf(msg.get("from"));
        var type = String.valueOf(msg.get("type"));
        var timestampStr = String.valueOf(msg.get("timestamp"));

        // Deduplicate: If we've already seen this message ID, ignore it.
        if (!processedMessages.add(messageId)) {
            log.info("Ignoring duplicate WhatsApp message: {}", messageId);
            return;
        }

        // Currently only handle text messages
        if (!"text".equals(type)) {
            log.info("Ignoring non-text WhatsApp message type={} from={}", type, from);
            return;
        }

        // Extract text body
        var textObj = (Map<String, Object>) msg.get("text");
        var body = textObj != null ? String.valueOf(textObj.get("body")) : "";

        log.info("Received WhatsApp text message from={}, messageId={}", from, messageId);

        // Parse timestamp (WhatsApp sends Unix epoch seconds as a string)
        var timestamp = parseTimestamp(timestampStr);

        // ─────────────────────────────────────────
        // Step 1: Normalize — Convert WhatsApp message → InboundMessage
        // ─────────────────────────────────────────
        var inboundMessage = new InboundMessage(
                from, // channelId = sender's phone number
                Optional.empty(), // WhatsApp doesn't have threads like Slack
                from, // userId = sender's phone number
                body, // message text
                new ChannelType.WhatsApp(phoneNumberId), // source
                timestamp,
                Map.of("waMessageId", messageId)); // platform-specific metadata

        try {
            // ─────────────────────────────────────────
            // Step 2: Process — Delegate to the agent service
            // ─────────────────────────────────────────
            var outboundMessage = agentService.process(inboundMessage);

            // ─────────────────────────────────────────
            // Step 3: Respond — Send the response back via the channel adapter
            // ─────────────────────────────────────────
            channelAdapter.sendMessage(outboundMessage);
        } catch (Exception e) {
            log.error("Error processing WhatsApp message from={}: {}", from, e.getMessage(), e);
        }
    }

    /**
     * Parses a WhatsApp timestamp string to an {@link Instant}.
     *
     * <h3>WhatsApp Timestamp Format</h3>
     * <p>
     * WhatsApp sends timestamps as Unix epoch seconds in string form
     * (e.g., {@code "1234567890"}). This is simpler than Slack's
     * {@code "1234567890.123456"} format.
     * </p>
     *
     * @param timestampStr the timestamp string (Unix epoch seconds)
     * @return the parsed Instant, or Instant.now() if parsing fails
     */
    private static Instant parseTimestamp(String timestampStr) {
        try {
            if (timestampStr != null && !timestampStr.isBlank()) {
                return Instant.ofEpochSecond(Long.parseLong(timestampStr));
            }
        } catch (NumberFormatException e) {
            // Fall through to default
        }
        return Instant.now();
    }
}
