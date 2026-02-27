package dev.prasadgaikwad.openclaw4j.channel;

/**
 * Represents the type of messaging channel the agent is communicating through.
 *
 * <p>
 * This <b>sealed interface</b> defines the set of supported channel types in
 * OpenClaw4J.
 * By using a sealed structure, we enable <b>exhaustive pattern matching</b> (a
 * Java 21+ feature),
 * ensuring that the compiler verifies all possible channel types are handled in
 * {@code switch}
 * expressions or {@code if} chains using pattern matching.
 * </p>
 *
 * <p>
 * Each permitted implementation is a {@code record}, providing an immutable,
 * type-safe
 * way to carry channel-specific metadata (like workspace IDs or phone numbers).
 * </p>
 *
 * <h3>Switch Expression Example:</h3>
 * 
 * <pre>
 * String description = switch (channelType) {
 *     case ChannelType.Slack(var id) -&gt; "Slack Workspace: " + id;
 *     case ChannelType.Console() -&gt; "Local Terminal";
 *     case ChannelType.Discord(var id) -&gt; "Discord Guild: " + id;
 *     case ChannelType.WhatsApp(var id) -&gt; "WhatsApp Number: " + id;
 * };
 * </pre>
 *
 * @author Prasad Gaikwad
 * @see InboundMessage
 * @see OutboundMessage
 */
public sealed interface ChannelType {

    /**
     * Represents a Slack channel connection.
     *
     * @param workspaceId the Slack workspace (team) identifier
     */
    record Slack(String workspaceId) implements ChannelType {
    }

    /**
     * Represents a Discord channel connection.
     * (Placeholder for MVP Slice 7)
     *
     * @param guildId the Discord server (guild) identifier
     */
    record Discord(String guildId) implements ChannelType {
    }

    /**
     * Represents a WhatsApp channel connection.
     *
     * @param phoneNumberId the WhatsApp Business phone number identifier
     * @see dev.prasadgaikwad.openclaw4j.channel.whatsapp.WhatsAppChannelAdapter
     */
    record WhatsApp(String phoneNumberId) implements ChannelType {
    }

    /**
     * Represents a console channel connection.
     */
    record Console() implements ChannelType {
    }
}
