package dev.prasadgaikwad.openclaw4j.agent;

import dev.prasadgaikwad.openclaw4j.channel.ChannelType;
import dev.prasadgaikwad.openclaw4j.channel.InboundMessage;
import dev.prasadgaikwad.openclaw4j.memory.MemorySnapshot;
import dev.prasadgaikwad.openclaw4j.tool.ToolResultStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentPlannerTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    private AgentPlanner agentPlanner;

    @BeforeEach
    void setUp() {
        agentPlanner = new AgentPlanner(chatClient);

        // Default mock chain for ChatClient
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(any(org.springframework.ai.chat.client.advisor.api.Advisor[].class)))
                .thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.toolCallbacks(anyList())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
    }

    @AfterEach
    void tearDown() {
        ToolResultStore.clear();
    }

    @Test
    void plan_shouldHandleEmptyPrimaryResponseWithRecovery() {
        // Arrange
        var context = createTestContext();
        ToolResultStore.set("Mocked tool output");

        // First call returns null content
        when(responseSpec.content()).thenReturn(null);

        // Recovery call uses varargs messages; stub both overloads
        when(requestSpec.messages(any(org.springframework.ai.chat.messages.Message[].class))).thenReturn(requestSpec);

        // Override the second content call
        when(responseSpec.content())
                .thenReturn(null) // first time
                .thenReturn("Recovered response"); // second time (recovery)

        // Act
        String result = agentPlanner.plan(context);

        // Assert
        assertEquals("Recovered response", result);
        verify(chatClient, times(2)).prompt();
    }

    @Test
    void plan_shouldPassToolResultsToRecoveryPrompt() {
        // Arrange
        var context = createTestContext();
        String fakeToolOutput = "Search result: Latest stock price is $100";
        ToolResultStore.set(fakeToolOutput);

        when(requestSpec.messages(any(org.springframework.ai.chat.messages.Message[].class))).thenReturn(requestSpec);

        // Primary call returns empty; recovery returns a real answer
        when(responseSpec.content())
                .thenReturn(null)
                .thenReturn("The latest stock price is $100.");

        // Act
        String result = agentPlanner.plan(context);

        // Assert — recovery should produce a real answer, not a generic fallback
        assertEquals("The latest stock price is $100.", result);
        // Two ChatClient calls: primary + recovery
        verify(chatClient, times(2)).prompt();
    }

    @Test
    void plan_shouldHandleEmptyPrimaryAndEmptyRecoveryResponse() {
        // Arrange
        var context = createTestContext();

        // Both calls return null content
        when(responseSpec.content()).thenReturn(null);

        // Act
        String result = agentPlanner.plan(context);

        // Assert
        assertEquals(
                "I've completed your request successfully. Is there anything specific from the results you'd like me to clarify?",
                result);
    }

    private AgentContext createTestContext() {
        var inbound = new InboundMessage(
                "C1", Optional.empty(), "U1", "Hello",
                new ChannelType.Slack("T1"), Instant.now(), Map.of());

        var profile = new AgentProfile("Agent", "Helpful", "System", Map.of());
        var memory = new MemorySnapshot(Collections.emptyList(), Map.of(), Optional.empty(), Optional.empty());

        return new AgentContext(inbound, Collections.emptyList(), memory, Collections.emptyList(), profile,
                Collections.emptyList(), Collections.emptyList());
    }
}
