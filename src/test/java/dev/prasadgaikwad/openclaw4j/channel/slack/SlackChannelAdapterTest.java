package dev.prasadgaikwad.openclaw4j.channel.slack;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import dev.prasadgaikwad.openclaw4j.channel.ChannelType;
import dev.prasadgaikwad.openclaw4j.channel.OutboundMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link SlackChannelAdapter}.
 *
 * <h2>Testing Strategy: Mocking External APIs</h2>
 * <p>
 * The Slack adapter depends on the Slack Web API ({@code MethodsClient}).
 * We don't want to make real API calls in tests, so we use
 * <strong>Mockito</strong> to create a mock {@code MethodsClient}.
 * </p>
 *
 * <p>
 * We then use an {@code ArgumentCaptor} to capture the request sent to
 * the mock, allowing us to verify that the adapter correctly translates
 * our {@code OutboundMessage} into a Slack API request.
 * </p>
 */
class SlackChannelAdapterTest {

        private MethodsClient mockMethodsClient;
        private SlackChannelAdapter adapter;

        @BeforeEach
        void setUp() {
                mockMethodsClient = mock(MethodsClient.class);
                adapter = new SlackChannelAdapter(mockMethodsClient);
        }

        @Test
        @DisplayName("Should send message to correct Slack channel")
        void sendMessage_shouldPostToCorrectChannel() throws IOException, SlackApiException {
                // Arrange — mock a successful API response
                var response = new ChatPostMessageResponse();
                response.setOk(true);
                when(mockMethodsClient.chatPostMessage(any(ChatPostMessageRequest.class)))
                                .thenReturn(response);

                var outbound = new OutboundMessage(
                                "C12345",
                                Optional.empty(),
                                "Hello from OpenClaw4J!",
                                new ChannelType.Slack("T11111"),
                                List.of());

                // Act
                adapter.sendMessage(outbound);

                // Assert — capture the request and verify its fields
                var captor = ArgumentCaptor.forClass(ChatPostMessageRequest.class);
                verify(mockMethodsClient).chatPostMessage(captor.capture());

                var capturedRequest = captor.getValue();
                assertEquals("C12345", capturedRequest.getChannel());
                assertEquals("Hello from OpenClaw4J!", capturedRequest.getText());
                assertNull(capturedRequest.getThreadTs(), "Should not have threadTs when no thread");
        }

        @Test
        @DisplayName("Should send threaded reply when threadId is present")
        void sendMessage_shouldSendThreadedReply() throws IOException, SlackApiException {
                // Arrange
                var response = new ChatPostMessageResponse();
                response.setOk(true);
                when(mockMethodsClient.chatPostMessage(any(ChatPostMessageRequest.class)))
                                .thenReturn(response);

                var outbound = new OutboundMessage(
                                "C12345",
                                Optional.of("1234567890.123456"),
                                "Threaded reply",
                                new ChannelType.Slack("T11111"),
                                List.of());

                // Act
                adapter.sendMessage(outbound);

                // Assert — verify thread timestamp is set
                var captor = ArgumentCaptor.forClass(ChatPostMessageRequest.class);
                verify(mockMethodsClient).chatPostMessage(captor.capture());

                var capturedRequest = captor.getValue();
                assertEquals("1234567890.123456", capturedRequest.getThreadTs());
        }

        @Test
        @DisplayName("Should handle Slack API errors gracefully")
        void sendMessage_shouldHandleApiErrors() throws IOException, SlackApiException {
                // Arrange — mock an error response
                var response = new ChatPostMessageResponse();
                response.setOk(false);
                response.setError("channel_not_found");
                when(mockMethodsClient.chatPostMessage(any(ChatPostMessageRequest.class)))
                                .thenReturn(response);

                var outbound = OutboundMessage.textReply(
                                "C_INVALID",
                                Optional.empty(),
                                "This should fail gracefully",
                                new ChannelType.Slack("T11111"));

                // Act — should not throw
                assertDoesNotThrow(() -> adapter.sendMessage(outbound));
        }

        @Test
        @DisplayName("Should handle IOException gracefully")
        void sendMessage_shouldHandleIOException() throws IOException, SlackApiException {
                // Arrange — mock a network failure
                when(mockMethodsClient.chatPostMessage(any(ChatPostMessageRequest.class)))
                                .thenThrow(new IOException("Network error"));

                var outbound = OutboundMessage.textReply(
                                "C12345",
                                Optional.empty(),
                                "Network will fail",
                                new ChannelType.Slack("T11111"));

                // Act — should not throw despite network error
                assertDoesNotThrow(() -> adapter.sendMessage(outbound));
        }

        @Test
        @DisplayName("Channel type should be Slack")
        void channelType_shouldBeSlack() {
                var type = adapter.channelType();
                assertInstanceOf(ChannelType.Slack.class, type);
        }
}
