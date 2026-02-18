package dev.prasadgaikwad.openclaw4j.channel.slack;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Slack channel adapter.
 *
 * <p>
 * This record uses {@link ConfigurationProperties} to bind Slack-specific
 * configuration
 * from {@code application.yml} (and environment variables) to immutable Java
 * fields.
 * It leverages Spring Boot's constructor-based binding for records.
 * </p>
 *
 * <h3>Example YAML Configuration:</h3>
 * 
 * <pre>
 * openclaw4j:
 *   channel:
 *     slack:
 *       bot-token: ${SLACK_BOT_TOKEN}
 *       signing-secret: ${SLACK_SIGNING_SECRET}
 * </pre>
 *
 * @param botToken      the Slack Bot User OAuth Token (starts with xoxb-)
 * @param signingSecret the Slack app's signing secret for request verification
 *
 * @author Prasad Gaikwad
 */
@ConfigurationProperties(prefix = "openclaw4j.channel.slack")
public record SlackProperties(
        String botToken,
        String signingSecret) {
}
