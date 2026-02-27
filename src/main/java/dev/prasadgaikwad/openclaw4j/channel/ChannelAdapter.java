package dev.prasadgaikwad.openclaw4j.channel;

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
     * Returns the channel type this adapter handles.
     *
     * @return the channel type identifier
     */
    ChannelType channelType();
}
