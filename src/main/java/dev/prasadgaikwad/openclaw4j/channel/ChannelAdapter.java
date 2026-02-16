package dev.prasadgaikwad.openclaw4j.channel;

/**
 * Defines the contract for all channel adapters in the OpenClaw4J system.
 *
 * <h2>Design Note: Why Not Sealed Here?</h2>
 * <p>
 * Ideally, we'd use a {@code sealed interface} to restrict implementations.
 * However, in Java without the module system (unnamed modules), a sealed type
 * can only permit classes in the <strong>same package</strong>. Since our
 * adapter
 * implementations live in sub-packages (e.g., {@code channel.slack}), we use
 * a regular interface here.
 * </p>
 *
 * <p>
 * The trade-off: we lose compile-time exhaustiveness in {@code switch}
 * expressions.
 * We compensate with clear package convention — each sub-package under
 * {@code channel/}
 * contains exactly one adapter implementation. In a modular (JPMS) build, this
 * interface could be sealed with cross-package permits.
 * </p>
 *
 * <h2>Design Pattern: Adapter</h2>
 * <p>
 * Each implementation translates between a platform-specific API and the
 * agent's normalized message types ({@link InboundMessage},
 * {@link OutboundMessage}).
 * The agent core only speaks in these normalized types — it never touches
 * Slack, Discord, or WhatsApp APIs directly.
 * </p>
 *
 * @see dev.prasadgaikwad.openclaw4j.channel.slack.SlackChannelAdapter
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
