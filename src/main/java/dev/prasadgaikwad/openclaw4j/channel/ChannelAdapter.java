package dev.prasadgaikwad.openclaw4j.channel;

import java.util.Optional;

/**
 * Defines the core contract for all channel-specific adapters in OpenClaw4J.
 *
 * <p>
 * This interface follows the <b>Adapter Design Pattern</b>. Its primary role is
 * to bridge the
 * gap between the platform-agnostic agent core and external communication
 * platforms (e.g., Slack, Discord).
 * </p>
 *
 * <p>
 * Implementations are responsible for:
 * </p>
 * <ul>
 * <li>Translating platform-specific payloads into normalized
 * {@link InboundMessage} records.</li>
 * <li>Converting the agent's normalized {@link OutboundMessage} records into
 * platform-specific API calls.</li>
 * <li>Exposing their {@link ChannelType} for registration and routing.</li>
 * </ul>
 *
 * <h3>Example Usage in a Controller/Handler:</h3>
 * 
 * <pre>
 * // Received a Slack event
 * InboundMessage normalizedInput = slackAdapter.normalize(slackEvent);
 * OutboundMessage response = agentService.process(normalizedInput);
 *
 * // Dispatch back through the adapter
 * slackAdapter.sendMessage(response);
 * </pre>
 *
 * @author Prasad Gaikwad
 * @see dev.prasadgaikwad.openclaw4j.channel.slack.SlackChannelAdapter
 * @see dev.prasadgaikwad.openclaw4j.channel.console.ConsoleChannelAdapter
 * @see dev.prasadgaikwad.openclaw4j.channel.whatsapp.WhatsAppChannelAdapter
 */
public interface ChannelAdapter {

    /**
     * Sends a message to the channel.
     *
     * <p>
     * The adapter translates the normalized {@code OutboundMessage}
     * into the platform-specific API call.
     * </p>
     *
     * @param message the normalized message to send
     */
    void sendMessage(OutboundMessage message);

    /**
     * Posts an immediate progress indicator (e.g., "⏳ Thinking...") before the
     * agent has finished computing its response.
     *
     * <p>
     * Returns an {@link Optional} containing the platform-specific message
     * identifier (e.g., Slack's {@code ts}) which can be used later to
     * update the message in-place via
     * {@link #updateMessage(String, Optional, String, String)}.
     * </p>
     *
     * <p>
     * The default implementation is a no-op, so channel adapters that do not
     * support message-updating (e.g., Console, WhatsApp) are unaffected.
     * </p>
     *
     * @param channelId target channel
     * @param threadId  optional thread to reply in
     * @param text      the progress indicator text to display
     * @return an Optional containing the message ID/ts if posted, or empty
     */
    default Optional<String> sendProgressMessage(String channelId, Optional<String> threadId, String text) {
        return Optional.empty();
    }

    /**
     * Updates a previously posted message in-place with new content.
     *
     * <p>
     * Used together with {@link #sendProgressMessage} to replace the
     * "Thinking..." placeholder with the final agent response. The default
     * implementation falls back to posting a new message.
     * </p>
     *
     * @param channelId  the channel containing the message to update
     * @param threadId   optional thread the message belongs to
     * @param messageTs  the platform-specific ID/ts of the message to update
     * @param newContent the new text content to replace the original message
     */
    default void updateMessage(String channelId, Optional<String> threadId, String messageTs, String newContent) {
        // Default: fall back to posting a new message
        sendMessage(OutboundMessage.textReply(channelId, threadId, newContent, channelType()));
    }

    /**
     * Returns the channel type this adapter handles.
     *
     * @return the channel type identifier
     */
    ChannelType channelType();
}
