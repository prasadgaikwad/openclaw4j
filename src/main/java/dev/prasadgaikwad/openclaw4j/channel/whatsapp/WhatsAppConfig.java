package dev.prasadgaikwad.openclaw4j.channel.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Spring configuration class that sets up the WhatsApp Cloud API client.
 *
 * <p>
 * This class is responsible for:
 * </p>
 * <ul>
 * <li>Activating {@link WhatsAppProperties} for configuration binding.</li>
 * <li>Creating a pre-configured {@link RestClient} bean with the WhatsApp
 * Cloud API base URL and Bearer token authentication.</li>
 * </ul>
 *
 * <h3>WhatsApp Cloud API vs Slack Bolt SDK</h3>
 * <p>
 * Unlike Slack which requires the Bolt SDK with its own servlet, WhatsApp's
 * Cloud API is a straightforward REST API. We use Spring's {@code RestClient}
 * (introduced in Spring Boot 3.2) for outbound calls, and a standard
 * {@code @RestController} ({@link WhatsAppWebhookController}) for inbound
 * webhooks. This keeps our dependency footprint minimal — no external SDK
 * needed.
 * </p>
 *
 * <h3>Spring Boot Concept: RestClient</h3>
 * <p>
 * {@code RestClient} is Spring Framework 6.1's modern, fluent HTTP client that
 * replaces {@code RestTemplate}. It provides a builder-style API for
 * configuring
 * base URLs, default headers, and interceptors — perfect for per-service
 * clients.
 * </p>
 *
 * @author Prasad Gaikwad
 * @see WhatsAppProperties
 * @see WhatsAppChannelAdapter
 * @see WhatsAppWebhookController
 */
@Configuration
@EnableConfigurationProperties(WhatsAppProperties.class)
public class WhatsAppConfig {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppConfig.class);

    private static final String GRAPH_API_BASE_URL = "https://graph.facebook.com";

    /**
     * Creates a pre-configured {@link RestClient} for the WhatsApp Cloud API.
     *
     * <h3>Why a Dedicated Bean?</h3>
     * <p>
     * By creating a named bean, we can:
     * </p>
     * <ul>
     * <li>Inject it into {@link WhatsAppChannelAdapter} for outbound messages</li>
     * <li>Pre-configure the Bearer token and base URL once</li>
     * <li>Mock it easily in tests without needing a real WhatsApp connection</li>
     * </ul>
     *
     * <h3>Graph API URL Format</h3>
     * <p>
     * The WhatsApp Cloud API endpoints follow the pattern:
     * {@code https://graph.facebook.com/{api-version}/{phone-number-id}/messages}
     * </p>
     *
     * @param properties the WhatsApp configuration properties
     * @return a RestClient configured for the WhatsApp Cloud API
     */
    @Bean("whatsappRestClient")
    public RestClient whatsappRestClient(WhatsAppProperties properties) {
        log.info("Configuring WhatsApp Cloud API client (API version: {})", properties.apiVersion());

        return RestClient.builder()
                .baseUrl(GRAPH_API_BASE_URL + "/" + properties.apiVersion())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
