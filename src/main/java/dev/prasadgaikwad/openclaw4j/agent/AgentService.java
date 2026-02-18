package dev.prasadgaikwad.openclaw4j.agent;

import dev.prasadgaikwad.openclaw4j.channel.InboundMessage;
import dev.prasadgaikwad.openclaw4j.channel.OutboundMessage;
import dev.prasadgaikwad.openclaw4j.memory.MemorySnapshot;
import dev.prasadgaikwad.openclaw4j.memory.ShortTermMemory;
import dev.prasadgaikwad.openclaw4j.tool.ToolRegistry;
import dev.prasadgaikwad.openclaw4j.memory.MemoryService;
import dev.prasadgaikwad.openclaw4j.memory.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

/**
 * The central orchestrator of the OpenClaw4J agent.
 *
 * <p>
 * This service acts as the primary "brain" that receives a platform-neutral
 * {@link InboundMessage},
 * coordinates with {@link ShortTermMemory} to retrieve conversation context,
 * assembles
 * an {@link AgentContext}, and delegates reasoning to the {@link AgentPlanner}.
 * The result is returned as an {@link OutboundMessage}.
 * </p>
 *
 * <p>
 * It provides a channel-agnostic processing pipeline, allowing the agent to
 * function
 * identically across Slack, Console, or any other supported communication
 * platform.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * 
 * <pre>
 * InboundMessage message = SlackChannelAdapter.normalize(event);
 * OutboundMessage response = agentService.process(message);
 * slackAdapter.reply(response);
 * </pre>
 *
 * @author Prasad Gaikwad
 * @see InboundMessage
 * @see OutboundMessage
 * @see AgentPlanner
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final AgentPlanner agentPlanner;
    private final ShortTermMemory shortTermMemory;
    private final MemoryService memoryService;
    private final ProfileService profileService;

    private final ToolRegistry toolRegistry;

    public AgentService(AgentPlanner agentPlanner,
            ShortTermMemory shortTermMemory,
            MemoryService memoryService,
            ProfileService profileService,
            ToolRegistry toolRegistry) {
        this.agentPlanner = agentPlanner;
        this.shortTermMemory = shortTermMemory;
        this.memoryService = memoryService;
        this.profileService = profileService;
        this.toolRegistry = toolRegistry;
    }

    /**
     * Processes an inbound message and returns the agent's response.
     *
     * <p>
     * This is the main entry point called by channel adapters.
     * </p>
     *
     * @param message the normalized inbound message from any channel
     * @return the agent's response, ready to be sent back via the channel adapter
     */
    @SuppressWarnings("null")
    public OutboundMessage process(InboundMessage message) {
        log.info("Processing message from user={} in channel={}: {}",
                message.userId(), message.channelId(), truncate(message.content(), 100));

        // 1. Determine context ID (thread ID or channel ID)
        String contextId = message.threadId().orElse(message.channelId());

        // 2. Retrieve conversation history
        var history = shortTermMemory.getHistory(contextId);

        // 3. Load Memory and Profile (Slice 4)
        var profile = profileService.getProfile();
        var relevantMemories = memoryService.getRelevantMemories();

        var memorySnapshot = new MemorySnapshot(
                relevantMemories,
                profile.preferences(),
                Optional.of(profile.agentPersonality()),
                Optional.empty());

        // 4. Build Agent Context
        var context = new AgentContext(
                message,
                history,
                memorySnapshot,
                Collections.emptyList(),
                profile,
                toolRegistry.getLocalTools(),
                toolRegistry.getMcpTools());

        // 5. Plan and Generate Response
        String responseText = agentPlanner.plan(context);

        // Fallback for empty responses to avoid IllegalArgumentException in
        // OutboundMessage
        if (responseText == null || responseText.isBlank()) {
            log.warn("Agent generated an empty response. Using fallback message.");
            responseText = "I've processed your request, but I don't have a specific response to provide at the moment. Is there anything else I can help with?";
        }

        // 6. Update Short-Term Memory
        shortTermMemory.addMessage(contextId, new UserMessage(message.content()));
        shortTermMemory.addMessage(contextId, new AssistantMessage(responseText));

        // 7. Log raw event to daily memory (Slice 4)
        memoryService.logEvent(String.format("Interaction with %s: Input='%s' | Response='%s'",
                message.userId(), truncate(message.content(), 50), truncate(responseText, 50)));

        log.info("Agent response for channel={}: {}",
                message.channelId(), truncate(responseText, 100));

        return OutboundMessage.textReply(
                message.channelId(),
                message.threadId(),
                responseText,
                message.source());
    }

    /**
     * Truncates a string to the given max length, appending "..." if truncated.
     * Used for safe logging of potentially long message content.
     *
     * @param text      the text to truncate
     * @param maxLength maximum number of characters
     * @return the truncated text
     */
    private static String truncate(String text, int maxLength) {
        if (text == null)
            return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
