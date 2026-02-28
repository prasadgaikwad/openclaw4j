package dev.prasadgaikwad.openclaw4j.channel.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the WhatsApp channel adapter.
 *
 * <p>
 * This record uses {@link ConfigurationProperties} to bind WhatsApp-specific
 * configuration from {@code application.yml} (and environment variables) to
 * immutable Java fields. It leverages Spring Boot's constructor-based binding
 * for records.
 * </p>
 *
 * <h3>Example YAML Configuration:</h3>
 * 
 * <pre>
 * openclaw4j:
 *   channel:
 *     whatsapp:
 *       access-token: ${WHATSAPP_ACCESS_TOKEN}
 *       phone-number-id: ${WHATSAPP_PHONE_NUMBER_ID}
 *       verify-token: ${WHATSAPP_VERIFY_TOKEN}
 *       api-version: v21.0
 * </pre>
 *
 * <h3>WhatsApp Cloud API Authentication:</h3>
 * <p>
 * The WhatsApp Business Cloud API uses a <b>Bearer Token</b> for
 * authentication,
 * unlike Slack which uses a Bot Token. The {@code accessToken} is obtained from
 * the Meta Developer Dashboard under your WhatsApp Business App's settings.
 * </p>
 *
 * @param accessToken   the WhatsApp Cloud API permanent access token
 * @param phoneNumberId the WhatsApp Business phone number ID (not the phone
 *                      number itself)
 * @param verifyToken   the webhook verification token (you define this; Meta
 *                      echoes it back)
 * @param apiVersion    the Graph API version to use (e.g., "v21.0")
 *
 * @author Prasad Gaikwad
 * @see WhatsAppConfig
 * @see WhatsAppChannelAdapter
 */
@ConfigurationProperties(prefix = "openclaw4j.channel.whatsapp")
public record WhatsAppProperties(
        String accessToken,
        String phoneNumberId,
        String verifyToken,
        String apiVersion) {

    /**
     * Compact constructor with defaults.
     *
     * <h3>Java 25 Concept: Compact Constructors with Defaults</h3>
     * <p>
     * We use a compact constructor to provide a sensible default for
     * {@code apiVersion} when it is not explicitly configured.
     * </p>
     */
    public WhatsAppProperties {
        if (apiVersion == null || apiVersion.isBlank()) {
            apiVersion = "v21.0";
        }
    }
}
