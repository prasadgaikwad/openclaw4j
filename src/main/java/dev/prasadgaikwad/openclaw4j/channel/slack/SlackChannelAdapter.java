package dev.prasadgaikwad.openclaw4j.channel.slack;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import dev.prasadgaikwad.openclaw4j.channel.ChannelAdapter;
import dev.prasadgaikwad.openclaw4j.channel.ChannelType;
import dev.prasadgaikwad.openclaw4j.channel.OutboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Slack-specific implementation of the {@link ChannelAdapter} interface.
 *
 * <p>
 * This adapter manages <b>outbound</b> communication by translating normalized
 * {@link OutboundMessage} records into Slack-specific {@code chat.postMessage}
 * API calls
 * using the {@link MethodsClient}.
 * </p>
 *
 * <p>
 * Note: <b>Inbound</b> Slack events (messages, mentions) are handled separately
 * by
 * Bolt event handlers configured in {@link SlackAppConfig}.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * 
 * <pre>
 * // Sending a threaded reply
 * OutboundMessage reply = OutboundMessage.textReply("C123", Optional.of("162..."), "Hi!", slackType);
 * slackChannelAdapter.sendMessage(reply);
 * </pre>
 *
 * @author Prasad Gaikwad
 * @see ChannelAdapter
 * @see SlackAppConfig
 */
@Component
public final class SlackChannelAdapter implements ChannelAdapter {

    private static final Logger log = LoggerFactory.getLogger(SlackChannelAdapter.class);

    private final MethodsClient methodsClient;

    /**
     * Creates a new SlackChannelAdapter.
     *
     * @param methodsClient the Slack Web API client, configured with the bot token.
     *                      This bean is created in {@link SlackAppConfig}.
     */
    public SlackChannelAdapter(MethodsClient methodsClient) {
        this.methodsClient = methodsClient;
    }

    /**
     * Sends a message to a Slack channel or thread.
     *
     * <h3>Slack API: chat.postMessage</h3>
     * <p>
     * This method translates our normalized {@link OutboundMessage} into a
     * Slack {@code chat.postMessage} API call. If a {@code threadId} is present,
     * the reply is posted as a threaded response.
     * </p>
     *
     * @param message the normalized outbound message to send
     */
    @Override
    public void sendMessage(OutboundMessage message) {
        try {
            // Build the Slack API request from our normalized OutboundMessage.
            // Note how the builder pattern maps cleanly to our record fields.
            var requestBuilder = ChatPostMessageRequest.builder()
                    .channel(message.channelId())
                    .text(message.content());

            // If the message is part of a thread, reply in that thread.
            // Optional.ifPresent() is a functional way to conditionally apply a value.
            message.threadId().ifPresent(requestBuilder::threadTs);

            var response = methodsClient.chatPostMessage(requestBuilder.build());

            if (response.isOk()) {
                log.debug("Message sent to Slack channel={}, thread={}",
                        message.channelId(), message.threadId().orElse("none"));
            } else {
                log.error("Slack API error: {}", response.getError());
            }
        } catch (IOException | SlackApiException e) {
            log.error("Failed to send message to Slack channel={}: {}",
                    message.channelId(), e.getMessage(), e);
        }
    }

    /**
     * Returns the channel type this adapter handles.
     *
     * <p>
     * We use an empty workspace ID here because the workspace is determined
     * by the bot token, not by this adapter. In a multi-workspace deployment,
     * you would have one adapter per workspace.
     * </p>
     *
     * @return a Slack channel type
     */
    @Override
    public ChannelType channelType() {
        return new ChannelType.Slack("");
    }
}
