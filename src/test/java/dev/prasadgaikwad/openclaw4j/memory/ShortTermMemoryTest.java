package dev.prasadgaikwad.openclaw4j.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShortTermMemoryTest {

    private ShortTermMemory shortTermMemory;

    @BeforeEach
    void setUp() {
        shortTermMemory = new ShortTermMemory();
    }

    @Test
    void shouldStoreAndRetrieveMessages() {
        String contextId = "test-context";
        UserMessage message1 = new UserMessage("Hello");
        AssistantMessage message2 = new AssistantMessage("Hi there!");

        shortTermMemory.addMessage(contextId, message1);
        shortTermMemory.addMessage(contextId, message2);

        List<Message> history = shortTermMemory.getHistory(contextId);
        assertThat(history).hasSize(2);
        assertThat(history.get(0)).isEqualTo(message1);
        assertThat(history.get(1)).isEqualTo(message2);
    }

    @Test
    void shouldEvictOldMessagesWhenTokenLimitExceeded() {
        String contextId = "overflow-context";

        // Add a very large message (limit is 4000 tokens)
        // A long string of "a " repeated many times
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append("word ");
        }
        String longContent = sb.toString();

        shortTermMemory.addMessage(contextId, new UserMessage("First small message"));
        shortTermMemory.addMessage(contextId, new UserMessage(longContent));

        List<Message> history = shortTermMemory.getHistory(contextId);

        // The first message should be evicted because the second one alone is likely >
        // 4000 tokens
        // and addMessage loop continues until currentTokens <= limit OR size == 1.
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getText()).isEqualTo(longContent);
    }

    @Test
    void shouldClearHistory() {
        String contextId = "clear-context";
        shortTermMemory.addMessage(contextId, new UserMessage("Clear me"));
        shortTermMemory.clear(contextId);
        assertThat(shortTermMemory.getHistory(contextId)).isEmpty();
    }
}
