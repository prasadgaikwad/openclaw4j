package dev.prasadgaikwad.openclaw4j.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import dev.prasadgaikwad.openclaw4j.tool.ToolResultStore;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The orchestration component responsible for generating a response or plan
 * based on the
 * {@link AgentContext}.
 *
 * <p>
 * This planner uses Spring AI's {@link ChatClient} to interact with the LLM. It
 * coordinates
 * the transformation of the current context into a series of messages (System,
 * History, User)
 * and leverages a {@link ToolCallAdvisor} to handle the internal <b>ReAct</b>
 * loop for
 * automatic tool execution.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * 
 * <pre>
 * AgentPlanner planner = new AgentPlanner(chatClient);
 * String result = planner.plan(assembledContext);
 * </pre>
 *
 * @author Prasad Gaikwad
 */
@Service
public class AgentPlanner {

    private static final Logger logger = LoggerFactory.getLogger(AgentPlanner.class);
    private final ChatClient chatClient;

    public AgentPlanner(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Decision-making loop that generates a text response or executes tools.
     *
     * <p>
     * This method assembles a prompt from the {@link AgentContext} and calls the
     * LLM.
     * It uses {@link ToolCallAdvisor} to automatically handle any tool calls
     * requested
     * by the model, returning the final synthesized answer.
     * </p>
     * 
     * <p>
     * Annotated with {@code @Retryable} to handle transient model or tool failures
     * during the complex ReAct cycle.
     * </p>
     *
     * @param context the full context for decision making
     * @return the generated response text, which may be the result of multiple tool
     *         calls
     */
    @SuppressWarnings("null")
    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public String plan(AgentContext context) {
        logger.debug("Planning response for context with {} history messages", context.conversationHistory().size());

        // 1. Mandatory Agent Rules (always included)
        String coreInstructions = """

                ### MANDATORY AGENT RULES:
                1. ANALYZE: Carefully read the user request and any context.
                2. BREAK DOWN: Split complex tasks into logical, sequential steps.
                3. TOOL USAGE: Execute tools one by one. Use the results of each tool to decide your next move.
                4. FINAL SYNTHESIS (CRITICAL): Once you have enough information, you MUST provide a final, helpful, and concise answer to the user.
                   - NEVER end your response with just tool JSON or empty content.
                   - If you used search results, summarize them in your own words.
                   - Even if the task is complete, say so explicitly.
                """;

        String systemPromptIdentity = context.profile().systemPrompt();
        if (systemPromptIdentity == null || systemPromptIdentity.isBlank()) {
            systemPromptIdentity = "You are OpenClaw4J, a powerful and autonomous AI agent.";
        }

        StringBuilder fullSystemPrompt = new StringBuilder(systemPromptIdentity);
        fullSystemPrompt.append(coreInstructions);

        // Append Long-Term Memory (Slice 4)
        List<String> memories = context.memory().relevantMemories();
        if (memories != null && !memories.isEmpty()) {
            fullSystemPrompt.append("\n### Long-Term Memory (Relevant Facts):\n");
            memories.forEach(m -> fullSystemPrompt.append("- ").append(m).append("\n"));
        }

        // Append Profile Directive (Slice 4)
        context.memory().soulDirective().ifPresent(d -> {
            fullSystemPrompt.append("\n### Soul Directive:\n").append(d).append("\n");
        });

        // Append RAG Documents (Slice 5)
        if (context.ragDocuments() != null && !context.ragDocuments().isEmpty()) {
            fullSystemPrompt.append("\n### Relevant Knowledge (from history/docs):\n");
            context.ragDocuments().forEach(doc -> {
                fullSystemPrompt.append("- ").append(doc.getText()).append("\n");
            });
        }

        // Append Current Context (Slice 6)
        String nowWithOffset = OffsetDateTime.now().toString();
        fullSystemPrompt.append("\n### Current Context:\n")
                .append("- User ID: ").append(context.message().userId()).append("\n")
                .append("- Channel ID: ").append(context.message().channelId()).append("\n");
        context.message().threadId()
                .ifPresent(tid -> fullSystemPrompt.append("- Thread ID: ").append(tid).append("\n"));
        fullSystemPrompt.append("- Current Time: ").append(nowWithOffset).append("\n");

        String finalPrompt = fullSystemPrompt.toString();
        logger.trace("Final System Prompt: {}", finalPrompt);

        // 2. Prepare messages
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(finalPrompt));

        // Add history
        messages.addAll(context.conversationHistory());

        // Add current user message (not yet in history/ShortTermMemory at this point)
        messages.add(new UserMessage(context.message().content()));

        // 3. Primary LLM call (includes full tool-calling ReAct loop via advisor)
        String response = chatClient.prompt()
                .advisors(ToolCallAdvisor.builder().build())
                .messages(messages)
                .tools(context.localTools().toArray())
                .toolCallbacks(context.mcpTools())
                .call()
                .content();

        if (logger.isDebugEnabled()) {
            logger.debug("Primary LLM response: {}", truncate(response, 200));
        }

        // ── Option 3: Recovery re-prompt ──────────────────────────────────────────
        // If the primary call returned an empty response (model forgot to synthesize
        // after tool calls), fire a lightweight follow-up asking for a summary.
        // This is a safety net; the stronger prompt above should prevent it in most
        // cases, but the recovery ensures the user always gets a meaningful reply.
        if (response == null || response.isBlank()) {
            logger.warn("Primary LLM response was empty — firing recovery re-prompt to summarize tool results.");
            response = fireRecoveryPrompt(context.message().content(), ToolResultStore.get());
        }

        return response;
    }

    /**
     * A follow-up LLM call used when the primary planning response is empty.
     *
     * <p>
     * When tool results are available (e.g. from {@link dev.prasadgaikwad.openclaw4j.tool.ToolResultStore}),
     * they are embedded directly into the system prompt so the model can
     * produce an accurate synthesis rather than a generic confirmation.
     * </p>
     *
     * @param originalUserInput the original message from the user
     * @param toolResults       the raw tool output captured during the ReAct loop,
     *                          or {@code null} if no tool was called
     * @return a non-empty summary string, or a safe fallback if the recovery also fails
     */
    @SuppressWarnings("null")
    private String fireRecoveryPrompt(String originalUserInput, String toolResults) {
        try {
            StringBuilder recoverySystemPromptBuilder = new StringBuilder(
                    "You are a helpful assistant. The user asked a question and a tool was executed "
                    + "to answer it. Your job is to write a clear, friendly, and concise response "
                    + "to the user based on the information below.");

            if (toolResults != null && !toolResults.isBlank()) {
                recoverySystemPromptBuilder
                        .append("\n\n### Tool Results:\n")
                        .append(toolResults)
                        .append("\n\nUsing only the information above, answer the user's question. "
                                + "Be specific, informative, and concise. Do not say that you 'fetched' "
                                + "or 'searched' for results — just present the information naturally.");
            } else {
                recoverySystemPromptBuilder
                        .append(" Summarise what you just did in one or two sentences. Be specific and concise.");
            }

            String recoveryUserPrompt = String.format(
                    "User's original request: \"%s\"", originalUserInput);

            String recovered = chatClient.prompt()
                    .messages(
                            new SystemMessage(recoverySystemPromptBuilder.toString()),
                            new UserMessage(recoveryUserPrompt))
                    .call()
                    .content();

            if (recovered != null && !recovered.isBlank()) {
                logger.info("Recovery re-prompt succeeded: {}", truncate(recovered, 200));
                return recovered;
            }
        } catch (Exception e) {
            logger.error("Recovery re-prompt also failed: {}", e.getMessage(), e);
        }

        // Ultimate fallback if recovery also produces nothing
        return "I've completed your request successfully. Is there anything specific from the results you'd like me to clarify?";
    }

    /**
     * Truncates a string for logging.
     */
    private String truncate(String text, int maxLength) {
        if (text == null)
            return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
