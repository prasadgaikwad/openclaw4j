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
 * <h2>Responsibility</h2>
 * <p>
 * This service receives a normalized {@link InboundMessage}, processes it,
 * and returns a normalized {@link OutboundMessage}. It is the single entry
 * point
 * for all agent logic, regardless of which channel the message came from.
 * </p>
 *
 * <h2>MVP Slice 2: Intelligence</h2>
 * <p>
 * In this slice, the agent uses {@link AgentPlanner} and Spring AI to generate
 * intelligent responses based on conversation history and a system prompt.
 * The simple echo behavior from Slice 1 has been replaced.
 * </p>
 *
 * <h2>Functional Style</h2>
 * <p>
 * The processing pipeline is expressed as a
 * {@code Function<InboundMessage, OutboundMessage>}.
 * This makes the agent's behavior composable and testable — you can swap the
 * processing function without changing the service's structure.
 * </p>
 *
 * <h2>Future Slices</h2>
 * <ul>
 * <li><strong>Slice 3</strong>: Add MCP tool orchestration</li>
 * <li><strong>Slice 4</strong>: Inject memory context before processing</li>
 * </ul>
 *
 * @see InboundMessage
 * @see OutboundMessage
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
                toolRegistry.getTools());

        // 4. Plan and Generate Response
        String responseText = agentPlanner.plan(context);

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
