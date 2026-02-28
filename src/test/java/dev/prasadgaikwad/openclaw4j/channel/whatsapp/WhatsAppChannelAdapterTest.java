package dev.prasadgaikwad.openclaw4j.channel.whatsapp;

import dev.prasadgaikwad.openclaw4j.channel.ChannelType;
import dev.prasadgaikwad.openclaw4j.channel.OutboundMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 *
 * <h2>Testing Strategy: MockRestServiceServer</h2>
 * <p>
 * Unlike the Slack adapter (which mocks the {@code MethodsClient}),
 * the WhatsApp adapter uses Spring's {@code RestClient}. We use
 * {@link MockRestServiceServer} to intercept and verify outbound HTTP
 * requests without hitting the real WhatsApp Cloud API.
 * </p>
 *
 * <h3>RestClient Testing Pattern</h3>
 * <p>
 * {@code MockRestServiceServer} works with the underlying
 * {@code RestTemplate} or {@code RestClient.Builder}. We bind a mock
 * server to the builder, so all requests made through the RestClient
 * are intercepted and can be verified.
 * </p>
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
        // Arrange — mock a successful API response
        mockServer.expect(requestTo("https://graph.facebook.com/v21.0/123456789/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess("{\"messages\":[{\"id\":\"wamid.test\"}]}", MediaType.APPLICATION_JSON));

        var outbound = new OutboundMessage(
                "1234567890",
                Optional.empty(),
                "Hello from OpenClaw4J!",
                new ChannelType.WhatsApp("123456789"),
                List.of());

        // Act
        adapter.sendMessage(outbound);

        // Assert — verify the request was made
        mockServer.verify();
    }

    @Test
    @DisplayName("Should handle API errors gracefully")
    void sendMessage_shouldHandleApiErrors() {
        // Arrange — mock an error response
        mockServer.expect(requestTo("https://graph.facebook.com/v21.0/123456789/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"Invalid phone number\"}}"));

        var outbound = OutboundMessage.textReply(
                "INVALID_NUMBER",
                Optional.empty(),
                "This should fail gracefully",
                new ChannelType.WhatsApp("123456789"));

        // Act — should not throw
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
