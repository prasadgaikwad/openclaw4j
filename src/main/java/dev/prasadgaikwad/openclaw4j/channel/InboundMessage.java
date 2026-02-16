package dev.prasadgaikwad.openclaw4j.channel;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * A normalized, immutable representation of a message received from any
 * channel.
 *
 * <h2>Java 25 Concept: Records</h2>
 * <p>
 * Records are a special kind of class that act as transparent carriers for
 * immutable data. The compiler automatically generates:
 * </p>
 * <ul>
 * <li>A canonical constructor</li>
 * <li>Accessor methods for each component (e.g., {@code channelId()})</li>
 * <li>{@code equals()}, {@code hashCode()}, and {@code toString()}</li>
 * </ul>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Every channel (Slack, Discord, WhatsApp) produces different event formats.
 * The channel adapter normalizes these into an {@code InboundMessage}, creating
 * a uniform interface for the agent core. This is the <strong>Adapter
 * Pattern</strong>
 * — translating a platform-specific interface into a common one.
 * </p>
 *
 * <p>
 * Using {@code Optional<String>} for {@code threadId} makes it explicit that
 * a message may or may not belong to a thread. This avoids null checks
 * scattered
 * across the codebase.
 * </p>
 *
 * @param channelId the channel/conversation identifier (platform-specific)
 * @param threadId  the thread identifier, if this message is part of a thread
 * @param userId    the user who sent the message
 * @param content   the text content of the message
 * @param source    which channel type this message came from
 * @param timestamp when the message was received
 * @param metadata  additional platform-specific data (e.g., Slack's event_id)
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
