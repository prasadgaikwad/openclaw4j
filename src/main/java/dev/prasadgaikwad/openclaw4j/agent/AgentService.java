package dev.prasadgaikwad.openclaw4j.agent;

import dev.prasadgaikwad.openclaw4j.channel.InboundMessage;
import dev.prasadgaikwad.openclaw4j.channel.OutboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.function.Function;

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
 * <h2>MVP Slice 1: Echo Mode</h2>
 * <p>
 * In this slice, the agent simply echoes back the received message.
 * This validates the full pipeline: Slack → Adapter → Agent → Adapter → Slack.
 * In Slice 2, this will be replaced with LLM-powered reasoning.
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
 * <li><strong>Slice 2</strong>: Replace echo with LLM-powered ChatClient</li>
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

    /**
     * The processing pipeline that transforms an inbound message into an outbound
     * response.
     *
     * <h3>Functional Programming: Function Composition</h3>
     * <p>
     * By defining the pipeline as a {@code Function}, we can easily compose
     * multiple processing steps in future slices:
     * </p>
     * 
     * <pre>{@code
     * Function<InboundMessage, OutboundMessage> pipeline = enrichWithMemory
     *         .andThen(enrichWithRAG)
     *         .andThen(planWithLLM)
     *         .andThen(executeTools)
     *         .andThen(composeResponse);
     * }</pre>
     */
    private final Function<InboundMessage, OutboundMessage> processingPipeline;

    /**
     * Creates the AgentService with the default echo pipeline.
     */
    public AgentService() {
        // Slice 1: Simple echo pipeline.
        // This will be replaced with a multi-stage LLM pipeline in Slice 2.
        this.processingPipeline = this::echo;
    }

    /**
     * Processes an inbound message and returns the agent's response.
     *
     * <p>
     * This is the main entry point called by channel adapters. It delegates
     * to the configured processing pipeline.
     * </p>
     *
     * @param message the normalized inbound message from any channel
     * @return the agent's response, ready to be sent back via the channel adapter
     */
    public OutboundMessage process(InboundMessage message) {
        log.info("Processing message from user={} in channel={}: {}",
                message.userId(), message.channelId(), truncate(message.content(), 100));

        var response = processingPipeline.apply(message);

        log.info("Agent response for channel={}: {}",
                message.channelId(), truncate(response.content(), 100));

        return response;
    }

    /**
     * Echo pipeline — returns the message content with a friendly prefix.
     *
     * <h3>Design Note</h3>
     * <p>
     * Even though this is a simple echo, we follow the full pipeline pattern:
     * receive → process → respond. This proves the entire message flow works
     * end-to-end before we add complexity.
     * </p>
     *
     * @param message the inbound message to echo
     * @return an outbound message echoing the content
     */
    private OutboundMessage echo(InboundMessage message) {
        var responseText = """
                🦞 **OpenClaw4J received your message:**
                > %s

                _I'm currently in echo mode (Slice 1). \
                LLM-powered responses coming in Slice 2!_\
                """.formatted(message.content());

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
