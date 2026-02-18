package dev.prasadgaikwad.openclaw4j.memory;

import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of Short-Term Memory (STM).
 *
 * <p>
 * This component stores conversation history (a list of {@link Message}
 * objects)
 * partitioned by a unique {@code contextId}. The {@code contextId} typically
 * corresponds to a Slack channel ID or a thread ID.
 * </p>
 *
 * <p>
 * To manage resource consumption, the memory implements a simple sliding-window
 * eviction policy, retaining only the last {@value #MAX_HISTORY_SIZE} messages
 * per context.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * 
 * <pre>
 * // Storing a message
 * shortTermMemory.addMessage("C12345", new UserMessage("Hello!"));
 *
 * // Retrieving history
 * List&lt;Message&gt; history = shortTermMemory.getHistory("C12345");
 * </pre>
 *
 * @author Prasad Gaikwad
 */
@Component
public class ShortTermMemory {

    private final Map<String, List<Message>> conversationHistory = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_SIZE = 50; // Simple eviction policy

    public void addMessage(String contextId, Message message) {
        conversationHistory.compute(contextId, (key, history) -> {
            if (history == null) {
                history = new ArrayList<>();
            }
            history.add(message);
            // Simple eviction: keep last N messages
            if (history.size() > MAX_HISTORY_SIZE) {
                return new ArrayList<>(history.subList(history.size() - MAX_HISTORY_SIZE, history.size()));
            }
            return history;
        });
    }

    public List<Message> getHistory(String contextId) {
        return Collections.unmodifiableList(conversationHistory.getOrDefault(contextId, Collections.emptyList()));
    }

    public void clear(String contextId) {
        conversationHistory.remove(contextId);
    }
}
