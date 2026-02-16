package dev.prasadgaikwad.openclaw4j.channel.slack;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Slack channel adapter.
 *
 * <h2>Spring Boot 4.0.2 Concept: @ConfigurationProperties with Records</h2>
 * <p>
 * Spring Boot supports using Java records as {@code @ConfigurationProperties}
 * targets.
 * This gives us:
 * </p>
 * <ul>
 * <li><strong>Immutability</strong> — properties are set once at startup and
 * never change</li>
 * <li><strong>Type safety</strong> — the compiler enforces the property
 * types</li>
 * <li><strong>Constructor binding</strong> — Spring automatically binds YAML
 * properties
 * to the record's constructor parameters</li>
 * </ul>
 *
 * <h2>How Property Binding Works</h2>
 * <p>
 * The YAML path {@code openclaw4j.channel.slack.bot-token} is bound to the
 * {@code botToken} parameter using Spring's relaxed binding rules:
 * </p>
 * <ul>
 * <li>{@code bot-token} (kebab-case in YAML) → {@code botToken} (camelCase in
 * Java)</li>
 * </ul>
 *
 * <h2>Security Note</h2>
 * <p>
 * The actual values come from environment variables (see
 * {@code application.yml}).
 * The {@code .env.example} file documents what variables to set. <strong>Never
 * hardcode credentials in source code or YAML files.</strong>
 * </p>
 *
 * @param botToken      the Slack Bot User OAuth Token (starts with xoxb-)
 * @param signingSecret the Slack app's signing secret for request verification
 */
@ConfigurationProperties(prefix = "openclaw4j.channel.slack")
public record SlackProperties(
                String botToken,
                String signingSecret) {
}
