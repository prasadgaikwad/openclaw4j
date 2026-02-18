package dev.prasadgaikwad.openclaw4j.agent;

import dev.prasadgaikwad.openclaw4j.channel.ChannelType;
import dev.prasadgaikwad.openclaw4j.channel.InboundMessage;
import dev.prasadgaikwad.openclaw4j.memory.ShortTermMemory;
import dev.prasadgaikwad.openclaw4j.tool.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link AgentService}.
 */
@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentPlanner agentPlanner;

    @Mock
    private ShortTermMemory shortTermMemory;

    @Mock
    private dev.prasadgaikwad.openclaw4j.memory.MemoryService memoryService;

    @Mock
    private dev.prasadgaikwad.openclaw4j.memory.ProfileService profileService;

    @Mock
    private ToolRegistry toolRegistry;

    private AgentService agentService;

    @BeforeEach
    void setUp() {
        agentService = new AgentService(agentPlanner, shortTermMemory, memoryService, profileService,
                toolRegistry);
    }

    @Nested
    @DisplayName("Message Processing")
    class MessageProcessing {

        @Test
        @DisplayName("Process should use planner to generate response")
        void process_shouldUsePlanner() {
            // Arrange
            var inbound = new InboundMessage(
                    "C12345",
                    Optional.empty(),
                    "U67890",
                    "Hello, Agent!",
                    new ChannelType.Slack("T11111"),
                    Instant.now(),
                    Map.of());

            var profile = new AgentProfile("Prasad", "Helpful", "Prompt", Collections.emptyMap());
            when(profileService.getProfile()).thenReturn(profile);
            when(memoryService.getRelevantMemories()).thenReturn(Collections.emptyList());
            when(shortTermMemory.getHistory(any())).thenReturn(Collections.emptyList());
            when(agentPlanner.plan(any())).thenReturn("Hello, User! I am OpenClaw4J.");

            // Act
            var outbound = agentService.process(inbound);

            // Assert
            assertNotNull(outbound);
            assertEquals("Hello, User! I am OpenClaw4J.", outbound.content());
            assertEquals("C12345", outbound.channelId());

            // Verify interactions
            verify(shortTermMemory).getHistory("C12345");
            verify(agentPlanner).plan(any(AgentContext.class));
            verify(shortTermMemory, times(2)).addMessage(eq("C12345"), any()); // User + Assistant messages
        }

        @Test
        @DisplayName("Process should preserve channel and thread IDs")
        void process_shouldPreserveChannelAndThread() {
            // Arrange
            var threadId = Optional.of("1234567890.123456");
            var inbound = new InboundMessage(
                    "C12345",
                    threadId,
                    "U67890",
                    "Hello from a thread",
                    new ChannelType.Slack("T11111"),
                    Instant.now(),
                    Map.of());

            var profile = new AgentProfile("Prasad", "Helpful", "Prompt", Collections.emptyMap());
            when(profileService.getProfile()).thenReturn(profile);
            when(memoryService.getRelevantMemories()).thenReturn(Collections.emptyList());
            when(shortTermMemory.getHistory(any())).thenReturn(Collections.emptyList());
            when(agentPlanner.plan(any())).thenReturn("Thread reply");

            // Act
            var outbound = agentService.process(inbound);

            // Assert
            assertEquals("C12345", outbound.channelId());
            assertEquals(threadId, outbound.threadId());
            verify(shortTermMemory).getHistory("1234567890.123456"); // Uses threadId as context
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCases {

        @Test
        @DisplayName("Process should handle planner exceptions")
        void shouldHandlePlannerException() {
            // Arrange
            var inbound = new InboundMessage("C1", Optional.empty(), "U1", "Hi", new ChannelType.Console(), Instant.now(), Map.of());
            when(profileService.getProfile()).thenReturn(new AgentProfile("A", "B", "C", Map.of()));
            when(agentPlanner.plan(any())).thenThrow(new RuntimeException("LLM failure"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> agentService.process(inbound));

            // In current implementation, messages are only added to history AFTER successful planning
            verify(shortTermMemory, never()).addMessage(any(), any());
        }
    }
}
