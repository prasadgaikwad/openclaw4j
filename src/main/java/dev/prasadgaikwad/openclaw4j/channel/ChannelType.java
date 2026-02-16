package dev.prasadgaikwad.openclaw4j.channel;

/**
 * Represents the type of messaging channel the agent is communicating through.
 *
 * <h2>Java 25 Concept: Sealed Interfaces</h2>
 * <p>
 * A sealed interface restricts which classes can implement it. This gives us
 * <strong>exhaustive pattern matching</strong> — the compiler knows all
 * possible
 * subtypes, so a {@code switch} expression can cover all cases without a
 * default branch.
 * </p>
 *
 * <h2>Java 25 Concept: Records as Sealed Permits</h2>
 * <p>
 * Each channel type is a {@code record} — an immutable, transparent data
 * carrier.
 * Records automatically provide {@code equals()}, {@code hashCode()}, and
 * {@code toString()}.
 * Combined with sealed interfaces, we get a type-safe, exhaustive union type.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * By sealing the channel types, we ensure that adding a new channel (e.g.,
 * WhatsApp)
 * forces us to handle it everywhere channels are pattern-matched. The compiler
 * catches any missed cases at build time, not at runtime.
 * </p>
 *
 * <h3>Usage example with pattern matching:</h3>
 * 
 * <pre>{@code
 * String label = switch (channelType) {
 *     case Slack slack -> "Slack workspace";
 *     case Discord discord -> "Discord server";
 *     case WhatsApp wa -> "WhatsApp chat";
 * };
 * }</pre>
 *
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
     * (Placeholder for future slice)
     *
     * @param phoneNumberId the WhatsApp Business phone number identifier
     */
    record WhatsApp(String phoneNumberId) implements ChannelType {
    }

    /**
     * Represents a console channel connection.
     */
    record Console() implements ChannelType {
    }
}
