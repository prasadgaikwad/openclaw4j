package dev.prasadgaikwad.openclaw4j.channel;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * A normalized, platform-neutral representation of a message received from any
 * communication channel.
 *
 * <p>
 * This <b>record</b> acts as a transparent data carrier, normalizing various
 * platform-specific
 * event formats (Slack, Discord, Console) into a uniform structure that the
 * agent core can process.
 * This implementation follows the <b>Adapter Design Pattern</b>.
 * </p>
 *
 * <h3>Example Construction:</h3>
 * 
 * <pre>
 * InboundMessage msg = new InboundMessage(
 *         "C123456",
 *         Optional.of("T7890"),
 *         "U999",
 *         "Fetch the latest logs",
 *         new ChannelType.Slack("T111"),
 *         Instant.now(),
 *         Map.of("event_id", "Ev123"));
 * </pre>
 *
 * @param channelId the channel or conversation identifier (platform-specific)
 * @param threadId  the thread identifier, if this message is part of a thread
 * @param userId    the user who sent the message
 * @param content   the text content of the message
 * @param source    which channel type this message came from
 * @param timestamp when the message was received
 * @param metadata  additional platform-specific metadata
 *
 * @author Prasad Gaikwad
 */
public record InboundMessage(
        String channelId,
        Optional<String> threadId,
        String userId,
        String content,
        ChannelType source,
        Instant timestamp,
        Map<String, String> metadata) {

    /**
     * Compact constructor for validation.
     *
     * <h3>Java 25 Concept: Compact Constructors</h3>
     * <p>
     * Records support compact constructors — a constructor body without
     * parameter list that runs validation before the fields are assigned.
     * This is the idiomatic way to enforce invariants on record data.
     * </p>
     */
    public InboundMessage {
        if (channelId == null || channelId.isBlank()) {
            throw new IllegalArgumentException("channelId must not be null or blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be null or blank");
        }
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        // Default timestamp to now if not provided
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        // Ensure metadata is never null — use empty map as default
        if (metadata == null) {
            metadata = Map.of();
        }
        // Ensure threadId is never null — use Optional.empty() as default
        if (threadId == null) {
            threadId = Optional.empty();
        }
    }
}
