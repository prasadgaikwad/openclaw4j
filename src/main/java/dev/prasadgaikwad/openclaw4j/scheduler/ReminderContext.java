package dev.prasadgaikwad.openclaw4j.scheduler;

import dev.prasadgaikwad.openclaw4j.channel.ChannelType;

import java.util.Optional;

/**
 * Holds the current message context needed to send reminder notifications.
 *
 * <p>
 * This is stored in a {@link ThreadLocal} and populated by {@code AgentService}
 * before each planning cycle. The {@code ReminderTool} reads from it so that
 * the LLM does not need to explicitly supply channel/user metadata as tool
 * arguments — reducing tool call complexity and improving reliability.
 * </p>
 *
 * <h3>Java 25 Concept: Scoped Values (simplified via ThreadLocal)</h3>
 * <p>
 * Java 25 offers {@code ScopedValue} for structured-concurrency-aware request
 * scoping. We use {@code ThreadLocal} here for compatibility with the
 * synchronous Bolt SDK thread model. A {@code ScopedValue} upgrade is a natural
 * follow-up once virtual-thread scoping is adopted throughout.
 * </p>
 *
 * @author Prasad Gaikwad
 */
public final class ReminderContext {

    private static final ThreadLocal<ReminderContext> CURRENT = new ThreadLocal<>();

    private final String channelId;
    private final Optional<String> threadId;
    private final String userId;
    private final ChannelType source;

    public ReminderContext(String channelId, Optional<String> threadId, String userId, ChannelType source) {
        this.channelId = channelId;
        this.threadId = threadId;
        this.userId = userId;
        this.source = source;
    }

    /** Set the reminder context for the current thread. */
    public static void set(ReminderContext ctx) {
        CURRENT.set(ctx);
    }

    /** Get the reminder context for the current thread. May be {@code null}. */
    public static ReminderContext get() {
        return CURRENT.get();
    }

    /** Clear the reminder context after the planning cycle completes. */
    public static void clear() {
        CURRENT.remove();
    }

    public String channelId() {
        return channelId;
    }

    public Optional<String> threadId() {
        return threadId;
    }

    public String userId() {
        return userId;
    }

    public ChannelType source() {
        return source;
    }
}
