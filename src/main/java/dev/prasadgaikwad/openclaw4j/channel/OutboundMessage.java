package dev.prasadgaikwad.openclaw4j.channel;

import java.util.List;
import java.util.Optional;

/**
 * A normalized, immutable representation of a message to be sent to a channel.
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Just as {@link InboundMessage} normalizes incoming platform events,
 * {@code OutboundMessage} normalizes outgoing responses. The channel adapter
 * translates this common format into platform-specific API calls
 * (e.g., Slack's {@code chat.postMessage}).
 * </p>
 *
 * <p>
 * This separation means the agent core never needs to know <em>how</em>
 * to send a Slack message — it just produces an {@code OutboundMessage},
 * and the adapter handles the rest.
 * </p>
 *
 * @param channelId   the target channel/conversation identifier
 * @param threadId    the thread to reply in, if applicable
 * @param content     the text content to send
 * @param destination which channel type to send to
 * @param attachments optional list of attachment descriptions
 */
public record OutboundMessage(
        String channelId,
        Optional<String> threadId,
        String content,
        ChannelType destination,
        List<String> attachments) {

    /**
     * Compact constructor with defaults and validation.
     */
    public OutboundMessage {
        if (channelId == null || channelId.isBlank()) {
            throw new IllegalArgumentException("channelId must not be null or blank");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be null or blank");
        }
        if (destination == null) {
            throw new IllegalArgumentException("destination must not be null");
        }
        if (threadId == null) {
            threadId = Optional.empty();
        }
        if (attachments == null) {
            attachments = List.of();
        }
    }

    /**
     * Convenience factory method to create a simple text reply.
     *
     * <h3>Functional Style: Static Factory Methods</h3>
     * <p>
     * Instead of complex constructors with many parameters, we provide
     * descriptive factory methods that make intent clear at the call site.
     * This is a common functional programming idiom.
     * </p>
     *
     * @param channelId   target channel
     * @param threadId    optional thread to reply in
     * @param content     message text
     * @param destination channel type
     * @return a new OutboundMessage with no attachments
     */
    public static OutboundMessage textReply(
            String channelId,
            Optional<String> threadId,
            String content,
            ChannelType destination) {
        return new OutboundMessage(channelId, threadId, content, destination, List.of());
    }
}
