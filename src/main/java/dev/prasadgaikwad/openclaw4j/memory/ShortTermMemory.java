package dev.prasadgaikwad.openclaw4j.memory;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
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
 * To manage resource consumption, the memory implements a token-aware context
 * management strategy, retaining messages until the total token count exceeds
 * {@value #MAX_TOKENS_FOR_HISTORY}.
 * </p>
 *
 * @author Prasad Gaikwad
 */
@Component
public class ShortTermMemory {

    private final Map<String, List<Message>> conversationHistory = new ConcurrentHashMap<>();
    private static final int MAX_TOKENS_FOR_HISTORY = 4000;
    private final Encoding encoding;

    public ShortTermMemory() {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        // O200K_BASE is used by GPT-4o models
        this.encoding = registry.getEncoding(EncodingType.O200K_BASE);
    }

    public void addMessage(String contextId, Message message) {
        conversationHistory.compute(contextId, (key, history) -> {
            if (history == null) {
                history = new ArrayList<>();
            }
            history.add(message);

            int currentTokens = countTokens(history);

            // Dynamic eviction: drop oldest messages while preserving the total token limit
            // We keep at least one message (the most recent one) even if it's very large
            while (currentTokens > MAX_TOKENS_FOR_HISTORY && history.size() > 1) {
                Message removed = history.remove(0);
                if (removed.getText() != null) {
                    currentTokens -= encoding.countTokens(removed.getText());
                }
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

    private int countTokens(List<Message> messages) {
        int total = 0;
        for (Message msg : messages) {
            String content = msg.getText();
            if (content != null) {
                total += encoding.countTokens(content);
            }
        }
        return total;
    }
}
