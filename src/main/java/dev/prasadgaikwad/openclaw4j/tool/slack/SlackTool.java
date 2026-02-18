package dev.prasadgaikwad.openclaw4j.tool.slack;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.conversations.ConversationsHistoryRequest;

import dev.prasadgaikwad.openclaw4j.tool.AITool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * A local tool enabling the agent to interact directly with Slack APIs.
 *
 * <p>
 * While the {@link SlackChannelAdapter} handles regular message dispatch, this
 * {@link AITool} provides the LLM with explicit capabilities to read channel
 * results (history) or post status updates independently of the main response
 * loop.
 * </p>
 *
 * <h3>Usage in LLM Prompt (Implicit):</h3>
 * <p>
 * The model decides when to call {@link #getChannelHistory(String, Integer)} or
 * {@link #postSlackMessage(String, String)} based on the tool descriptions
 * provided
 * in the {@code @Tool} annotations.
 * </p>
 *
 * @author Prasad Gaikwad
 * @see AITool
 * @see MethodsClient
 */
@Service
public class SlackTool implements AITool {

    private static final Logger logger = LoggerFactory.getLogger(SlackTool.class);
    private final MethodsClient methodsClient;

    public SlackTool(MethodsClient methodsClient) {
        this.methodsClient = methodsClient;
    }

    /**
     * Posts a message to a Slack channel.
     *
     * @param channelId The ID of the channel (e.g., 'C12345')
     * @param text      The message text
     * @return Confirmation or error
     */
    @Tool(name = "postSlackMessage", description = "Posts a message to a Slack channel. Use this for general communication or status updates.")
    public String postSlackMessage(String channelId, String text) {
        logger.info("Posting Slack message to channel {}: {}", channelId, text);
        try {
            var response = methodsClient.chatPostMessage(r -> r.channel(channelId).text(text));
            if (response.isOk()) {
                return "Message posted successfully to channel " + channelId;
            }
            return "Failed to post message: " + response.getError();
        } catch (Exception e) {
            logger.error("Error posting to Slack", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Gets the history of a Slack channel.
     *
     * @param channelId The ID of the channel
     * @param limit     Max number of messages to retrieve (default 10)
     * @return String representation of messages
     */
    @Tool(name = "getChannelHistory", description = "Retrieves recent messages from a Slack channel to gain context on a conversation or thread.")
    public String getChannelHistory(String channelId, Integer limit) {
        logger.info("Fetching Slack history for channel {}, limit={}", channelId, limit);
        int finalLimit = (limit != null && limit > 0) ? limit : 10;
        try {
            var response = methodsClient.conversationsHistory(ConversationsHistoryRequest.builder()
                    .channel(channelId)
                    .limit(finalLimit)
                    .build());

            if (response.isOk()) {
                return response.getMessages().stream()
                        .map(m -> String.format("[%s] %s: %s", m.getTs(), m.getUser(), m.getText()))
                        .collect(Collectors.joining("\n"));
            }
            return "Failed to fetch history: " + response.getError();
        } catch (Exception e) {
            logger.error("Error fetching Slack history", e);
            return "Error: " + e.getMessage();
        }
    }
}
