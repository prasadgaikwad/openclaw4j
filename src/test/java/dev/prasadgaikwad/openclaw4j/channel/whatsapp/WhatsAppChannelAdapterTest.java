package dev.prasadgaikwad.openclaw4j.channel.whatsapp;

import dev.prasadgaikwad.openclaw4j.channel.ChannelType;
import dev.prasadgaikwad.openclaw4j.channel.OutboundMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Unit tests for the {@link WhatsAppChannelAdapter}.
 */
class WhatsAppChannelAdapterTest {

        private MockRestServiceServer mockServer;
        private WhatsAppChannelAdapter adapter;

        @BeforeEach
        void setUp() {
                var properties = new WhatsAppProperties(
                                "test-access-token",
                                "123456789",
                                "test-verify-token",
                                "v21.0");

                var restClientBuilder = RestClient.builder()
                                .baseUrl("https://graph.facebook.com/v21.0");

                mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
                var restClient = restClientBuilder.build();

                adapter = new WhatsAppChannelAdapter(restClient, properties);
        }

        @Test
        @DisplayName("Should send text message to correct WhatsApp recipient")
        void sendMessage_shouldPostToCorrectRecipient() {
                // Arrange
                mockServer.expect(requestTo("https://graph.facebook.com/v21.0/123456789/messages"))
                                .andExpect(method(HttpMethod.POST))
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andRespond(withSuccess("{\"messages\":[{\"id\":\"wamid.test\"}]}",
                                                MediaType.APPLICATION_JSON));

                var outbound = new OutboundMessage(
                                "1234567890",
                                Optional.empty(),
                                "Hello from OpenClaw4J!",
                                new ChannelType.WhatsApp("123456789"),
                                List.of());

                // Act
                adapter.sendMessage(outbound);

                // Assert
                mockServer.verify();
        }

        @ParameterizedTest(name = "Should handle {0} error gracefully")
        @ValueSource(ints = { 400, 401, 500 })
        @DisplayName("Should handle API errors gracefully")
        void sendMessage_shouldHandleApiErrors(int statusCodeValue) {
                // Arrange
                var status = HttpStatus.valueOf(statusCodeValue);
                mockServer.expect(requestTo("https://graph.facebook.com/v21.0/123456789/messages"))
                                .andExpect(method(HttpMethod.POST))
                                .andRespond(withStatus(status)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .body("{\"error\":{\"message\":\"API Error\", \"code\": "
                                                                + statusCodeValue + "}}"));

                var outbound = OutboundMessage.textReply(
                                "INVALID_ID",
                                Optional.empty(),
                                "This should fail gracefully",
                                new ChannelType.WhatsApp("123456789"));

                // Act & Assert
                assertDoesNotThrow(() -> adapter.sendMessage(outbound));
        }

        @Test
        @DisplayName("Channel type should be WhatsApp with correct phone number ID")
        void channelType_shouldBeWhatsApp() {
                var type = adapter.channelType();
                assertInstanceOf(ChannelType.WhatsApp.class, type);
                assertEquals("123456789", ((ChannelType.WhatsApp) type).phoneNumberId());
        }
}
