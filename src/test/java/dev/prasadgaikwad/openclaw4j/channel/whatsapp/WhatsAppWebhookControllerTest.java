package dev.prasadgaikwad.openclaw4j.channel.whatsapp;

import dev.prasadgaikwad.openclaw4j.agent.AgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link WhatsAppWebhookController}.
 *
 * <h2>Testing Strategy: Webhook Verification</h2>
 * <p>
 * The WhatsApp webhook has two endpoints:
 * </p>
 * <ul>
 * <li><b>GET</b> — Verification: Meta sends a challenge, we echo it back if the
 * verify token matches.</li>
 * <li><b>POST</b> — Notifications: Meta sends message events, we process them
 * asynchronously.</li>
 * </ul>
 * <p>
 * We test the verification logic directly (unit test) and the POST endpoint
 * with mocked dependencies.
 * </p>
 */
class WhatsAppWebhookControllerTest {

    private WhatsAppWebhookController controller;
    private AgentService mockAgentService;
    private WhatsAppChannelAdapter mockAdapter;

    @BeforeEach
    void setUp() {
        mockAgentService = mock(AgentService.class);
        mockAdapter = mock(WhatsAppChannelAdapter.class);
        var properties = new WhatsAppProperties(
                "test-access-token",
                "123456789",
                "my-secret-verify-token",
                "v21.0");

        controller = new WhatsAppWebhookController(mockAgentService, mockAdapter, properties);
    }

    @Test
    @DisplayName("Should verify webhook with correct token and return challenge")
    void verifyWebhook_shouldReturnChallenge_whenTokenMatches() {
        // Act
        var response = controller.verifyWebhook("subscribe", "my-secret-verify-token", "challenge123");

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals("challenge123", response.getBody());
    }

    @Test
    @DisplayName("Should reject webhook verification with wrong token")
    void verifyWebhook_shouldReturn403_whenTokenDoesNotMatch() {
        // Act
        var response = controller.verifyWebhook("subscribe", "wrong-token", "challenge123");

        // Assert
        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Should reject webhook verification with wrong mode")
    void verifyWebhook_shouldReturn403_whenModeIsNotSubscribe() {
        // Act
        var response = controller.verifyWebhook("unsubscribe", "my-secret-verify-token", "challenge123");

        // Assert
        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Should accept POST webhook and return 200 immediately")
    void handleWebhook_shouldReturn200Immediately() {
        // Arrange — minimal valid payload (no messages)
        var payload = java.util.Map.<String, Object>of(
                "object", "whatsapp_business_account",
                "entry", java.util.List.of());

        // Act
        var response = controller.handleWebhook(payload);

        // Assert — should respond immediately with 200
        assertEquals(200, response.getStatusCode().value());
        assertEquals("EVENT_RECEIVED", response.getBody());
    }
}
