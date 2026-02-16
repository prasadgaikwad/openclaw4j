package dev.prasadgaikwad.openclaw4j.agent;

import dev.prasadgaikwad.openclaw4j.channel.ChannelType;
import dev.prasadgaikwad.openclaw4j.channel.InboundMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link AgentService}.
 *
 * <h2>Testing Strategy</h2>
 * <p>
 * Since the agent service is a pure function in Slice 1 (input → output,
 * no side effects), we can test it without any mocks or Spring context.
 * This is a key benefit of the functional programming style.
 * </p>
 */
class AgentServiceTest {

    private final AgentService agentService = new AgentService();

    @Test
    @DisplayName("Echo pipeline should return message with OpenClaw4J prefix")
    void process_shouldEchoMessageWithPrefix() {
        // Arrange — create a sample inbound message
        var inbound = new InboundMessage(
                "C12345",
                Optional.empty(),
                "U67890",
                "Hello, OpenClaw4J!",
                new ChannelType.Slack("T11111"),
                Instant.now(),
                Map.of());

        // Act — process the message through the agent
        var outbound = agentService.process(inbound);

        // Assert — the response should contain the original message
        assertNotNull(outbound);
        assertTrue(outbound.content().contains("Hello, OpenClaw4J!"),
                "Response should contain the original message text");
        assertTrue(outbound.content().contains("OpenClaw4J"),
                "Response should contain the OpenClaw4J identifier");
    }

    @Test
    @DisplayName("Echo pipeline should preserve channel and thread IDs")
    void process_shouldPreserveChannelAndThread() {
        // Arrange — message with a thread ID
        var threadId = Optional.of("1234567890.123456");
        var inbound = new InboundMessage(
                "C12345",
                threadId,
                "U67890",
                "Hello from a thread",
                new ChannelType.Slack("T11111"),
                Instant.now(),
                Map.of());

        // Act
        var outbound = agentService.process(inbound);

        // Assert — channel and thread should be carried through
        assertEquals("C12345", outbound.channelId());
        assertEquals(threadId, outbound.threadId());
    }

    @Test
    @DisplayName("Echo pipeline should preserve the source channel type")
    void process_shouldPreserveSourceChannelType() {
        // Arrange
        var slackSource = new ChannelType.Slack("T11111");
        var inbound = new InboundMessage(
                "C12345",
                Optional.empty(),
                "U67890",
                "Test message",
                slackSource,
                Instant.now(),
                Map.of());

        // Act
        var outbound = agentService.process(inbound);

        // Assert — the response destination should match the source
        assertEquals(slackSource, outbound.destination());
    }
}
