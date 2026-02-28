package dev.prasadgaikwad.openclaw4j.channel.whatsapp;

import dev.prasadgaikwad.openclaw4j.agent.AgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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

        @ParameterizedTest
        @CsvSource({
                        "subscribe, my-secret-verify-token, challenge123, 200, challenge123",
                        "subscribe, wrong-token, challenge123, 403, ",
                        "unsubscribe, my-secret-verify-token, challenge123, 403, "
        })
        @DisplayName("Should verify webhook correctly based on mode and token")
        void verifyWebhook_shouldReturnCorrectResponse(String mode, String token, String challenge, int expectedStatus,
                        String expectedBody) {
                // Act
                var response = controller.verifyWebhook(mode, token, challenge);

                // Assert
                assertEquals(expectedStatus, response.getStatusCode().value());
                if (expectedBody != null) {
                        assertEquals(expectedBody, response.getBody());
                }
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
