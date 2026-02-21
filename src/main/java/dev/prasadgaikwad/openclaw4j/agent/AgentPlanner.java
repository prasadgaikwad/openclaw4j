package dev.prasadgaikwad.openclaw4j.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.stereotype.Service;

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
     * @param context the full context for decision making
     * @return the generated response text, which may be the result of multiple tool
     *         calls
     */
    @SuppressWarnings("null")
    public String plan(AgentContext context) {
        logger.debug("Planning response for context with {} history messages", context.conversationHistory().size());

        String systemPromptText = context.profile().systemPrompt();
        if (systemPromptText == null || systemPromptText.isEmpty()) {
            systemPromptText = "You are OpenClaw4J, a helpful AI assistant.";
        }

        // 1. Prepare messages
        List<Message> messages = new ArrayList<>();

        StringBuilder fullSystemPrompt = new StringBuilder(systemPromptText);

        // Append Long-Term Memory (Slice 4)
        List<String> memories = context.memory().relevantMemories();
        if (memories != null && !memories.isEmpty()) {
            fullSystemPrompt.append("\n\n### Long-Term Memory (Relevant Facts):\n");
            memories.forEach(m -> fullSystemPrompt.append("- ").append(m).append("\n"));
        }

        // Append Profile Directive (Slice 4)
        context.memory().soulDirective().ifPresent(d -> {
            fullSystemPrompt.append("\n\n### Soul Directive:\n").append(d).append("\n");
        });

        // Append RAG Documents (Slice 5)
        if (context.ragDocuments() != null && !context.ragDocuments().isEmpty()) {
            fullSystemPrompt.append("\n\n### Relevant Knowledge (from history/docs):\n");
            context.ragDocuments().forEach(doc -> {
                fullSystemPrompt.append("- ").append(doc.getText()).append("\n");
            });
        }

        // Append Current Context (Slice 6)
        String nowWithOffset = OffsetDateTime.now().toString();
        fullSystemPrompt.append("\n\n### Current Context:\n")
                .append("- User ID: ").append(context.message().userId()).append("\n")
                .append("- Channel ID: ").append(context.message().channelId()).append("\n");
        context.message().threadId()
                .ifPresent(tid -> fullSystemPrompt.append("- Thread ID: ").append(tid).append("\n"));
        fullSystemPrompt.append("- Current Time: ").append(nowWithOffset).append("\n")
                .append("  (Use this as your reference when computing reminder times. ")
                .append("Always supply 'remindAt' as a full ISO-8601 datetime WITH timezone offset, ")
                .append("e.g. if current time is 2026-02-19T21:30:00-06:00 and user says ")
                .append("'in 5 minutes', supply 2026-02-19T21:35:00-06:00)\n");

        messages.add(new SystemMessage(fullSystemPrompt.toString()));

        // Add history
        messages.addAll(context.conversationHistory());

        // Add current user message (it is not yet in history/ShortTermMemory at this
        // point)
        messages.add(new UserMessage(context.message().content()));

        // 2. Call LLM
        String response = chatClient.prompt()
                .advisors(ToolCallAdvisor.builder().build())
                .messages(messages)
                .tools(context.localTools().toArray())
                .toolCallbacks(context.mcpTools())
                .call()
                .content();

        logger.debug("Generated response: {}", response);
        return response;
    }
}
