package dev.prasadgaikwad.openclaw4j.agent;

import dev.prasadgaikwad.openclaw4j.channel.InboundMessage;
import dev.prasadgaikwad.openclaw4j.memory.MemorySnapshot;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * The full context assembled for the agent to plan and execute a task.
 *
 * @param message             The inbound message that triggered this context
 * @param conversationHistory Short-term memory of the conversation
 * @param memory              Relevant long-term and working memory snapshot
 * @param ragDocuments        Relevant documents retrieved from RAG
 * @param profile             The agent's profile and system prompt
 * @param availableTools      List of available tools (names/definitions)
 */
public record AgentContext(
        InboundMessage message,
        List<Message> conversationHistory,
        MemorySnapshot memory,
        List<Document> ragDocuments,
        AgentProfile profile,
        List<String> availableTools) {
}
