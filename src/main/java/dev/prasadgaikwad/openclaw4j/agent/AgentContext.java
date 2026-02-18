package dev.prasadgaikwad.openclaw4j.agent;

import dev.prasadgaikwad.openclaw4j.channel.InboundMessage;
import dev.prasadgaikwad.openclaw4j.memory.MemorySnapshot;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * The assembled context used by the {@link AgentPlanner} to decide the next
 * action.
 *
 * <p>
 * This record serves as a "context object" that bundles all information the
 * agent
 * needs to reason about a request: the user's message, conversation history,
 * relevant memories, retrieved documents (RAG), and available tools.
 * </p>
 *
 * <h3>Example Construction:</h3>
 * 
 * <pre>
 * AgentContext context = new AgentContext(
 *         inboundMessage,
 *         history,
 *         memorySnapshot,
 *         ragDocs,
 *         agentProfile,
 *         localTools,
 *         mcpTools);
 * </pre>
 *
 * @param message             The inbound message that triggered this context
 * @param conversationHistory Short-term memory of the conversation
 * @param memory              Relevant long-term and working memory snapshot
 * @param ragDocuments        Relevant documents retrieved from RAG
 * @param profile             The agent's profile and system prompt
 * @param localTools          List of available local tools (names/definitions)
 * @param mcpTools            List of available MCP tools (names/definitions)
 *
 * @author Prasad Gaikwad
 */
public record AgentContext(
        InboundMessage message,
        List<Message> conversationHistory,
        MemorySnapshot memory,
        List<Document> ragDocuments,
        AgentProfile profile,
        List<Object> localTools,
        List<ToolCallback> mcpTools) {
}
