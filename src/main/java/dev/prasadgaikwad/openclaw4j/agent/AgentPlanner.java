package dev.prasadgaikwad.openclaw4j.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * The planner responsible for generating a response or plan based on the agent
 * context.
 * Uses Spring AI's ChatClient to interact with the LLM.
 */
@Service
public class AgentPlanner {

    private static final Logger logger = LoggerFactory.getLogger(AgentPlanner.class);
    private final ChatClient chatClient;

    public AgentPlanner(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Plans and executes the next step for the agent.
     * <p>
     * For MVP-2, this primarily generates a text response using conversation
     * history and the system prompt. In future slices, this will handle
     * tool execution loops (ReAct pattern).
     * </p>
     *
     * @param context the full context for decision making
     * @return the generated response text or thought process
     */
    public String plan(AgentContext context) {
        logger.debug("Planning response for context with {} history messages", context.conversationHistory().size());

        String systemPromptText = context.profile().systemPrompt();
        if (systemPromptText == null || systemPromptText.isEmpty()) {
            systemPromptText = "You are OpenClaw4J, a helpful AI assistant.";
        }

        // 1. Prepare messages
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPromptText));

        // Add history
        messages.addAll(context.conversationHistory());

        // Add current user message (it is not yet in history/ShortTermMemory at this
        // point)
        messages.add(new UserMessage(context.message().content()));

        // 2. Call LLM
        // In MVP-3 we will use .tools(context.availableTools())
        String response = chatClient.prompt()
                .messages(messages)
                .call()
                .content();

        logger.debug("Generated response: {}", response);
        return response;
    }
}
