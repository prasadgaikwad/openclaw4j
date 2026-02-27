package dev.prasadgaikwad.openclaw4j.channel.whatsapp;

import dev.prasadgaikwad.openclaw4j.channel.ChannelAdapter;
import dev.prasadgaikwad.openclaw4j.channel.ChannelType;
import dev.prasadgaikwad.openclaw4j.channel.OutboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * WhatsApp-specific implementation of the {@link ChannelAdapter} interface.
 *
 * <p>
 * This adapter manages <b>outbound</b> communication by translating normalized
 * {@link OutboundMessage} records into WhatsApp Cloud API {@code POST}
 * requests.
 * </p>
 *
 * <p>
 * Note: <b>Inbound</b> WhatsApp messages (webhooks) are handled separately by
 * {@link WhatsAppWebhookController}.
 * </p>
 *
 * <h3>WhatsApp Cloud API Message Format</h3>
 * <p>
 * The Cloud API expects a JSON payload like:
 * </p>
 * 
 * <pre>
 * {
 *   "messaging_product": "whatsapp",
 *   "recipient_type": "individual",
 *   "to": "1234567890",
 *   "type": "text",
 *   "text": { "body": "Hello from OpenClaw4J!" }
 * }
 * </pre>
 *
 * <h3>Design Decision: RestClient vs External SDK</h3>
 * <p>
 * Unlike Slack (which requires the Bolt SDK), WhatsApp's Cloud API is a
 * standard REST API. Using Spring's {@link RestClient} keeps the dependency
 * footprint minimal and leverages Spring's built-in HTTP error handling,
 * interceptors, and observability.
 * </p>
 *
 * @author Prasad Gaikwad
 * @see ChannelAdapter
 * @see WhatsAppConfig
 * @see WhatsAppWebhookController
 */
@Component
public final class WhatsAppChannelAdapter implements ChannelAdapter {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppChannelAdapter.class);

    private final RestClient restClient;
    private final WhatsAppProperties properties;

    /**
     * Creates a new WhatsAppChannelAdapter.
     *
     * @param restClient the pre-configured WhatsApp Cloud API client.
     *                   This bean is created in {@link WhatsAppConfig}.
     * @param properties the WhatsApp configuration properties.
     */
    public WhatsAppChannelAdapter(
            @Qualifier("whatsappRestClient") RestClient restClient,
            WhatsAppProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /**
     * Sends a message to a WhatsApp user.
     *
     * <h3>WhatsApp Cloud API: Send Message</h3>
     * <p>
     * This method translates our normalized {@link OutboundMessage} into a
     * WhatsApp Cloud API {@code POST /{phone-number-id}/messages} call.
     * The recipient is identified by the {@code channelId} field, which
     * should contain the user's phone number (with country code, no + prefix).
     * </p>
     *
     * <h3>API Response</h3>
     * <p>
     * On success, the API returns a JSON body with a {@code messages} array
     * containing the message ID. On error, it returns an {@code error} object.
     * We log both scenarios but do not throw — following the same graceful
     * error handling pattern as
     * {@link dev.prasadgaikwad.openclaw4j.channel.slack.SlackChannelAdapter}.
     * </p>
     *
     * @param message the normalized outbound message to send
     */
    @Override
    public void sendMessage(OutboundMessage message) {
        try {
            // Build the WhatsApp Cloud API message payload.
            // The channelId field contains the recipient's phone number.
            var payload = Map.of(
                    "messaging_product", "whatsapp",
                    "recipient_type", "individual",
                    "to", message.channelId(),
                    "type", "text",
                    "text", Map.of("body", message.content()));

            var response = restClient.post()
                    .uri("/{phoneNumberId}/messages", properties.phoneNumberId())
                    .body(payload)
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Message sent to WhatsApp recipient={}: {}",
                        message.channelId(), response.getBody());
            } else {
                log.error("WhatsApp API error (HTTP {}): {}",
                        response.getStatusCode(), response.getBody());
            }
        } catch (RestClientException e) {
            log.error("Failed to send message to WhatsApp recipient={}: {}",
                    message.channelId(), e.getMessage(), e);
        }
    }

    /**
     * Returns the channel type this adapter handles.
     *
     * <p>
     * The phone number ID is taken from the configuration properties.
     * </p>
     *
     * @return a WhatsApp channel type
     */
    @Override
    public ChannelType channelType() {
        return new ChannelType.WhatsApp(properties.phoneNumberId());
    }
}
