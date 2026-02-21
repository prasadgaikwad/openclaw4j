package dev.prasadgaikwad.openclaw4j.channel.console;

import dev.prasadgaikwad.openclaw4j.agent.AgentService;
import dev.prasadgaikwad.openclaw4j.channel.ChannelAdapter;
import dev.prasadgaikwad.openclaw4j.channel.ChannelType;
import dev.prasadgaikwad.openclaw4j.channel.InboundMessage;
import dev.prasadgaikwad.openclaw4j.channel.OutboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A local channel adapter that enables direct interaction with the agent via
 * the terminal.
 *
 * <p>
 * This adapter implements a <b>REPL (Read-Eval-Print Loop)</b>, allowing
 * developers to test
 * agent logic without connecting to external platforms like Slack. It runs in a
 * separate
 * thread to avoid blocking the main application context startup.
 * </p>
 *
 * <h3>Usage:</h3>
 * <p>
 * When the application starts, it will display a Lobster (🦞) prompt in the
 * terminal.
 * Simply type your message and press Enter.
 * </p>
 *
 * @author Prasad Gaikwad
 * @see ChannelAdapter
 * @see CommandLineRunner
 */
@Component
@Profile("console")
public class ConsoleChannelAdapter implements ChannelAdapter, CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ConsoleChannelAdapter.class);

    private final AgentService agentService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ConsoleChannelAdapter(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    public void run(String... args) {
        log.info("Starting Console Adapter...");
        executor.submit(this::startRepl);
    }

    private void startRepl() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("\n\n" +
                    "╔════════════════════════════════════════════════════╗\n" +
                    "║            🦞 OpenClaw4J Console Interface         ║\n" +
                    "║                                                    ║\n" +
                    "║  Type your message and press Enter.                ║\n" +
                    "║  Type 'exit' or 'quit' to stop the console loop.   ║\n" +
                    "╚════════════════════════════════════════════════════╝\n");

            while (true) {
                System.out.print("\n👉 You: ");
                if (!scanner.hasNextLine())
                    break;

                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                    System.out.println("👋 Exiting console mode...");
                    break;
                }

                if (input.isBlank())
                    continue;

                processInput(input);
            }
        }
    }

    private void processInput(String input) {
        try {
            // Normalize console input to InboundMessage
            var inbound = new InboundMessage(
                    "CONSOLE_CHANNEL",
                    Optional.empty(),
                    "CONSOLE_USER",
                    input,
                    new ChannelType.Console(),
                    Instant.now(),
                    Map.of());

            // Delegate to AgentService
            // Note: We don't catch the return value here directly because
            // the AgentService might (in future) be async. Instead, the
            // AgentService eventually calls sendMessage() on this adapter.
            // However, in Slice 1, it's synchronous, so we could just print result.
            // BUT, to request true "adapter" behavior, we rely on the agent calling
            // sendMessage.
            // Wait - AgentService.process() returns OutboundMessage.
            // So we DO handle it immediately here for the sync pipeline.
            var outbound = agentService.process(inbound);

            // Send the response (which prints to stdout)
            sendMessage(outbound);

        } catch (Exception e) {
            log.error("Error processing console command", e);
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    @Override
    public void sendMessage(OutboundMessage message) {
        // This is called by the agent to send a response back to the user
        System.out.println("\n🤖 OpenClaw4J:");
        System.out.println(message.content());
        System.out.println("────────────────────────────────────────");
    }

    @Override
    public ChannelType channelType() {
        return new ChannelType.Console();
    }
}
