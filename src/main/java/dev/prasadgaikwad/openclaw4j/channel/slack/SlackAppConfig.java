package dev.prasadgaikwad.openclaw4j.channel.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.jakarta_servlet.SlackAppServlet;
import com.slack.api.methods.MethodsClient;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.MessageEvent;
import dev.prasadgaikwad.openclaw4j.agent.AgentService;
import dev.prasadgaikwad.openclaw4j.channel.ChannelType;
import dev.prasadgaikwad.openclaw4j.channel.InboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Spring configuration that wires up the Slack Bolt SDK with the OpenClaw4J
 * agent.
 *
 * <h2>How Slack Bolt Works</h2>
 * <p>
 * Slack Bolt is an event-driven framework for building Slack apps. Here's the
 * flow:
 * </p>
 * <ol>
 * <li>Slack sends an HTTP POST to your app's webhook URL when events occur</li>
 * <li>Bolt automatically <strong>verifies the request signature</strong> using
 * the signing secret (prevents spoofing)</li>
 * <li>Bolt <strong>parses the event payload</strong> and dispatches it to
 * the matching handler</li>
 * <li>Your handler processes the event and calls {@code ctx.ack()} to
 * acknowledge receipt within 3 seconds (Slack's requirement)</li>
 * </ol>
 *
 * <h2>Spring Boot Integration</h2>
 * <p>
 * We register the Bolt {@code App} as a servlet at {@code /slack/events}.
 * Spring Boot's embedded Tomcat serves this servlet alongside the normal
 * Spring MVC endpoints (like Actuator).
 * </p>
 *
 * <h2>Jakarta EE Note</h2>
 * <p>
 * Spring Boot 4.0.2 uses Jakarta EE 11 (package {@code jakarta.servlet.*}).
 * We use {@code bolt-jakarta-servlet} instead of {@code bolt-servlet} to ensure
 * compatibility. This is a common migration concern when upgrading to Spring
 * Boot 4.x.
 * </p>
 *
 * <h2>Key Concepts</h2>
 * <ul>
 * <li><strong>App</strong> — the Bolt application instance; holds all event
 * handlers</li>
 * <li><strong>AppConfig</strong> — configuration (tokens, signing secret)</li>
 * <li><strong>SlackAppServlet</strong> — adapts the Bolt App to the Jakarta
 * Servlet API</li>
 * <li><strong>ctx.ack()</strong> — acknowledges the event; must be called
 * within 3 seconds</li>
 * </ul>
 *
 * @see <a href="https://slack.dev/java-slack-sdk/guides/bolt-basics">Bolt for
 *      Java Guide</a>
 */
@Configuration
@EnableConfigurationProperties(SlackProperties.class)
public class SlackAppConfig {

    private static final Logger log = LoggerFactory.getLogger(SlackAppConfig.class);

    // Cache to prevent duplicate processing of retried events.
    // In a production app, use a proper cache with TTL (e.g. Caffeine or Redis).
    private final Set<String> processedEvents = ConcurrentHashMap.newKeySet();

    // Executor for processing agent tasks asynchronously.
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    /**
     * Creates and configures the Slack Bolt {@code App} with event handlers.
     *
     * <h3>Event Handler: message</h3>
     * <p>
     * We listen for {@link MessageEvent} — fired whenever a message is posted
     * in a channel where the bot is present. The handler:
     * </p>
     * <ol>
     * <li>Ignores bot messages (to prevent echo loops)</li>
     * <li>Normalizes the Slack event into an {@link InboundMessage}</li>
     * <li>Delegates to {@link AgentService} for processing</li>
     * <li>Sends the response back via {@link SlackChannelAdapter}</li>
     * </ol>
     *
     * @param slackProperties Slack configuration (token, signing secret)
     * @param agentService    the agent service to process messages
     * @param channelAdapter  the Slack adapter to send responses
     * @return a configured Bolt App instance
     */
    @Bean
    public App slackApp(SlackProperties slackProperties,
            AgentService agentService,
            SlackChannelAdapter channelAdapter) {

        // Build the Bolt AppConfig from our Spring-managed properties.
        // This is where the bot token and signing secret are injected.
        var appConfig = AppConfig.builder()
                .singleTeamBotToken(slackProperties.botToken())
                .signingSecret(slackProperties.signingSecret())
                .build();

        var app = new App(appConfig);

        app.command("/go", (req, ctx) -> {
            return ctx.ack(":wave: Hello! Today is a good day to start something new.");
        });

        app.event(MessageEvent.class, (payload, ctx) -> {
            return ctx.ack();
        });

        // ─────────────────────────────────────────────
        // Register event handler: app_mention
        // ─────────────────────────────────────────────
        // This handler fires only when the bot is explicitly mentioned (@bot).
        app.event(AppMentionEvent.class, (payload, ctx) -> {
            var event = payload.getEvent();
            var eventId = payload.getEventId();

            // Deduplicate: If we've already seen this event_id, ignore it.
            if (!processedEvents.add(eventId)) {
                log.info("Ignoring duplicate Slack event: {}", eventId);
                return ctx.ack();
            }

            log.info("Received Slack mention from user={} in channel={}, eventId={}",
                    event.getUser(), event.getChannel(), eventId);

            // PROCESS ASYNCHRONOUSLY
            // Slack requires an ack within 3 seconds. LLM calls often take longer.
            // By moving the work to an executor, we can ack immediately.
            executor.submit(() -> {
                try {
                    // ─────────────────────────────────────────
                    // Step 1: Normalize — Convert Slack event → InboundMessage
                    // ─────────────────────────────────────────
                    var inboundMessage = new InboundMessage(
                            event.getChannel(),
                            Optional.ofNullable(event.getThreadTs()),
                            event.getUser(),
                            event.getText(),
                            new ChannelType.Slack(event.getTeam() != null ? event.getTeam() : ""),
                            parseSlackTimestamp(event.getTs()),
                            Map.of("slackTs", event.getTs() != null ? event.getTs() : ""));

                    // ─────────────────────────────────────────
                    // Step 2: Process — Delegate to the agent service
                    // ─────────────────────────────────────────
                    var outboundMessage = agentService.process(inboundMessage);

                    // ─────────────────────────────────────────
                    // Step 3: Respond — Send the response back via the channel adapter
                    // ─────────────────────────────────────────
                    channelAdapter.sendMessage(outboundMessage);

                } catch (Exception e) {
                    log.error("Error processing async Slack event", e);
                }
            });

            // Acknowledge the event. Slack requires this within 3 seconds.
            return ctx.ack();
        });

        return app;
    }

    /**
     * Creates the Slack Web API client (MethodsClient) used for sending messages.
     *
     * <h3>Why a Separate Bean?</h3>
     * <p>
     * The {@code MethodsClient} is extracted as a bean so it can be:
     * </p>
     * <ul>
     * <li>Injected into {@link SlackChannelAdapter} for outbound messages</li>
     * <li>Mocked in tests without needing a real Slack connection</li>
     * </ul>
     *
     * @param slackProperties the Slack configuration
     * @return a configured MethodsClient
     */
    @Bean
    public MethodsClient methodsClient(SlackProperties slackProperties) {
        return com.slack.api.Slack.getInstance()
                .methods(slackProperties.botToken());
    }

    /**
     * Registers the Bolt App as a servlet at {@code /slack/events}.
     *
     * <h3>Spring Boot Concept: ServletRegistrationBean</h3>
     * <p>
     * Since the Bolt App is not a Spring MVC controller, we register it
     * as a raw servlet. Spring Boot's embedded Tomcat serves it alongside
     * the MVC DispatcherServlet. This is how non-Spring web frameworks
     * coexist with Spring MVC in the same application.
     * </p>
     *
     * <h3>Jakarta EE Note</h3>
     * <p>
     * We use the Jakarta variant of {@code SlackAppServlet} from the
     * {@code bolt-jakarta-servlet} module, which extends
     * {@code jakarta.servlet.http.HttpServlet}
     * instead of the legacy {@code javax.servlet.http.HttpServlet}.
     * </p>
     *
     * @param app the Bolt App instance
     * @return a servlet registration for /slack/events
     */
    @Bean
    public ServletRegistrationBean<SlackAppServlet> slackAppServlet(App app) {
        var servlet = new SlackAppServlet(app);
        return new ServletRegistrationBean<>(servlet, "/slack/events");
    }

    /**
     * Parses Slack's unique timestamp format into a Java {@link Instant}.
     *
     * <h3>Slack Timestamp Format</h3>
     * <p>
     * Slack uses a format like {@code "1234567890.123456"} where the integer
     * part is Unix epoch seconds and the decimal part is microseconds.
     * We parse the seconds portion into an {@code Instant}.
     * </p>
     *
     * @param slackTs the Slack timestamp string
     * @return the parsed Instant, or Instant.now() if parsing fails
     */
    private static Instant parseSlackTimestamp(String slackTs) {
        try {
            if (slackTs != null && slackTs.contains(".")) {
                long epochSeconds = Long.parseLong(slackTs.split("\\.")[0]);
                return Instant.ofEpochSecond(epochSeconds);
            }
        } catch (NumberFormatException e) {
            // Fall through to default
        }
        return Instant.now();
    }
}
