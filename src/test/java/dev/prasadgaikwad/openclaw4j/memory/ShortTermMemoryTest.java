package dev.prasadgaikwad.openclaw4j.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShortTermMemory Unit Tests")
class ShortTermMemoryTest {

    private ShortTermMemory shortTermMemory;

    @BeforeEach
    void setUp() {
        shortTermMemory = new ShortTermMemory();
    }

    @Nested
    @DisplayName("Basic Operations")
    class BasicOperations {

        @ParameterizedTest
        @ValueSource(strings = {"C1", "C2", "Thread-123"})
        @DisplayName("Should store and retrieve messages for different contexts")
        void shouldStoreAndRetrieveMessages(String contextId) {
            Message message = new UserMessage("Hello in " + contextId);
            shortTermMemory.addMessage(contextId, message);

            List<Message> history = shortTermMemory.getHistory(contextId);
            assertEquals(1, history.size());
            assertEquals(message, history.get(0));
        }

        @Test
        @DisplayName("Should return empty list for unknown context")
        void shouldReturnEmptyForUnknownContext() {
            assertTrue(shortTermMemory.getHistory("unknown").isEmpty());
        }

        @Test
        @DisplayName("Should clear history for a context")
        void shouldClearHistory() {
            String contextId = "C1";
            shortTermMemory.addMessage(contextId, new UserMessage("Msg 1"));
            assertFalse(shortTermMemory.getHistory(contextId).isEmpty());

            shortTermMemory.clear(contextId);
            assertTrue(shortTermMemory.getHistory(contextId).isEmpty());
        }
    }

    @Nested
    @DisplayName("Eviction Policy")
    class EvictionPolicy {

        @Test
        @DisplayName("Should evict old messages when limit is reached")
        void shouldEvictOldMessages() {
            String contextId = "C1";
            // MAX_HISTORY_SIZE is 50
            for (int i = 0; i < 60; i++) {
                shortTermMemory.addMessage(contextId, new UserMessage("Message " + i));
            }

            List<Message> history = shortTermMemory.getHistory(contextId);
            assertEquals(50, history.size());
            assertEquals("Message 10", history.get(0).getText());
            assertEquals("Message 59", history.get(49).getText());
        }
    }

    @Nested
    @DisplayName("Concurrency and Thread Safety")
    class ConcurrencyTests {

        @Test
        @DisplayName("Should handle concurrent message additions")
        void shouldHandleConcurrentAdditions() throws InterruptedException {
            String contextId = "C-Concurrent";
            int numThreads = 10;
            int msgsPerThread = 20;

            Thread[] threads = new Thread[numThreads];
            for (int i = 0; i < numThreads; i++) {
                final int threadIdx = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < msgsPerThread; j++) {
                        shortTermMemory.addMessage(contextId, new UserMessage("T" + threadIdx + " M" + j));
                    }
                });
                threads[i].start();
            }

            for (Thread t : threads) {
                t.join();
            }

            List<Message> history = shortTermMemory.getHistory(contextId);
            // Since MAX_HISTORY_SIZE is 50, and we added 200 messages, it should be 50.
            assertEquals(50, history.size());
        }
    }

}
