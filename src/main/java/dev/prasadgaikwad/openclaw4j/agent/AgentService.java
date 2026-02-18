package dev.prasadgaikwad.openclaw4j.agent;

import dev.prasadgaikwad.openclaw4j.channel.InboundMessage;
import dev.prasadgaikwad.openclaw4j.channel.OutboundMessage;
import dev.prasadgaikwad.openclaw4j.memory.MemorySnapshot;
import dev.prasadgaikwad.openclaw4j.memory.ShortTermMemory;
import dev.prasadgaikwad.openclaw4j.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
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

    private final ToolRegistry toolRegistry;
    private final Resource systemPromptResource;

    public AgentService(AgentPlanner agentPlanner,
            ShortTermMemory shortTermMemory,
            ToolRegistry toolRegistry,
            @Value("classpath:prompts/system.prompt") Resource systemPromptResource) {
        this.agentPlanner = agentPlanner;
        this.shortTermMemory = shortTermMemory;
        this.toolRegistry = toolRegistry;
        this.systemPromptResource = systemPromptResource;
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
    public OutboundMessage process(InboundMessage message) {
        log.info("Processing message from user={} in channel={}: {}",
                message.userId(), message.channelId(), truncate(message.content(), 100));

        // 1. Determine context ID (thread ID or channel ID)
        String contextId = message.threadId().orElse(message.channelId());

        // 2. Retrieve conversation history
        var history = shortTermMemory.getHistory(contextId);

        // 3. Build Agent Context
        // Load system prompt from resource
        String systemPrompt = "You are a helpful assistant.";
        try {
            systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to load system prompt", e);
        }

        // For MVP-3, we inject tools from the registry
        var profile = new AgentProfile(
                "User",
                "Helpful Assistant",
                systemPrompt,
                Collections.emptyMap());

        var memorySnapshot = new MemorySnapshot(
                Collections.emptyList(),
                Collections.emptyMap(),
                Optional.empty(),
                Optional.empty());

        var context = new AgentContext(
                message,
                history,
                memorySnapshot,
                Collections.emptyList(),
                profile,
                toolRegistry.getLocalTools(),
                toolRegistry.getMcpTools());

        // 4. Plan and Generate Response
        String responseText = agentPlanner.plan(context);

        // Fallback for empty responses to avoid IllegalArgumentException in
        // OutboundMessage
        if (responseText == null || responseText.isBlank()) {
            log.warn("Agent generated an empty response. Using fallback message.");
            responseText = "I've processed your request, but I don't have a specific response to provide at the moment. Is there anything else I can help with?";
        }

        // 5. Update Short-Term Memory
        // Add User Message
        shortTermMemory.addMessage(contextId, new UserMessage(message.content()));
        // Add Agent Response
        shortTermMemory.addMessage(contextId, new AssistantMessage(responseText));

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
