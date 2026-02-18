package dev.prasadgaikwad.openclaw4j.channel;

import java.util.List;
import java.util.Optional;

/**
 * A normalized, platform-neutral representation of a message to be sent to a
 * channel.
 *
 * <p>
 * This record facilitates the <b>separation of concerns</b> between the agent
 * core and the
 * communication channels. The agent generates an {@code OutboundMessage}, and
 * the
 * appropriate {@link ChannelAdapter} handles its delivery to specific platform
 * APIs.
 * </p>
 *
 * <h3>Example Construction:</h3>
 * 
 * <pre>
 * OutboundMessage msg = OutboundMessage.textReply(
 *         "C12345",
 *         Optional.of("T6789"),
 *         "Hello from OpenClaw4J!",
 *         new ChannelType.Slack("T111"));
 * </pre>
 *
 * @param channelId   the target channel or conversation identifier (e.g., Slack
 *                    ID)
 * @param threadId    the thread to reply in, if applicable
 * @param content     the text content to send
 * @param destination which channel type to send to (e.g.,
 *                    {@link ChannelType.Slack})
 * @param attachments optional list of attachment identifiers or descriptions
 *
 * @author Prasad Gaikwad
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
