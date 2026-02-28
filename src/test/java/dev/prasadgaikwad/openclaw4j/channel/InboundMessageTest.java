package dev.prasadgaikwad.openclaw4j.channel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link InboundMessage} record.
 *
 * <h2>What We're Testing</h2>
 * <p>
 * Since {@code InboundMessage} is a record, Java auto-generates
 * {@code equals()}, {@code hashCode()}, {@code toString()}, and accessor
 * methods.
 * These tests verify:
 * </p>
 * <ul>
 * <li>Record creation with all fields</li>
 * <li>Compact constructor validation (rejecting invalid data)</li>
 * <li>Default values for optional fields (metadata, threadId)</li>
 * <li>Immutability guarantees</li>
 * </ul>
 */
class InboundMessageTest {

        @Test
        @DisplayName("Should create InboundMessage with all fields")
        void shouldCreateMessageWithAllFields() {
                var now = Instant.now();
                var metadata = Map.of("eventId", "abc123");
                var threadId = Optional.of("1234567890.123456");

                var message = new InboundMessage(
                                "C12345", threadId, "U67890", "Hello",
                                new ChannelType.Slack("T11111"), now, metadata);

                // Record accessors — no getXxx(), just fieldName()
                assertEquals("C12345", message.channelId());
                assertEquals(threadId, message.threadId());
                assertEquals("U67890", message.userId());
                assertEquals("Hello", message.content());
                assertEquals(now, message.timestamp());
                assertEquals(metadata, message.metadata());
        }

        @Test
        @DisplayName("Should default metadata to empty map when null")
        void shouldDefaultMetadataToEmptyMap() {
                var message = new InboundMessage(
                                "C12345", Optional.empty(), "U67890", "Hello",
                                new ChannelType.Slack("T11111"), Instant.now(), null);

                assertNotNull(message.metadata());
                assertTrue(message.metadata().isEmpty());
        }

        @Test
        @DisplayName("Should default threadId to Optional.empty when null")
        void shouldDefaultThreadIdToEmpty() {
                var message = new InboundMessage(
                                "C12345", null, "U67890", "Hello",
                                new ChannelType.Slack("T11111"), Instant.now(), Map.of());

                assertEquals(Optional.empty(), message.threadId());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "  ", "\t", "\n" })
        @DisplayName("Should reject null or blank channelId")
        void shouldRejectInvalidChannelId(String invalidId) {
                assertThrows(IllegalArgumentException.class,
                                () -> new InboundMessage(invalidId, Optional.empty(), "U67890", "Hello",
                                                new ChannelType.Slack("T11111"), Instant.now(), Map.of()));
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("Should reject null userId")
        void shouldRejectNullUserId(String nullUserId) {
                assertThrows(IllegalArgumentException.class,
                                () -> new InboundMessage("C12345", Optional.empty(), nullUserId, "Hello",
                                                new ChannelType.Slack("T11111"), Instant.now(), Map.of()));
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("Should reject null source channel type")
        void shouldRejectNullSource(ChannelType nullSource) {
                assertThrows(IllegalArgumentException.class,
                                () -> new InboundMessage("C12345", Optional.empty(), "U67890", "Hello",
                                                nullSource, Instant.now(), Map.of()));
        }

        @Test
        @DisplayName("Records should support value-based equality")
        void shouldSupportValueEquality() {
                var now = Instant.now();
                var msg1 = new InboundMessage("C12345", Optional.empty(), "U67890", "Hello",
                                new ChannelType.Slack("T11111"), now, Map.of());
                var msg2 = new InboundMessage("C12345", Optional.empty(), "U67890", "Hello",
                                new ChannelType.Slack("T11111"), now, Map.of());

                // Records provide structural equality out of the box
                assertEquals(msg1, msg2);
                assertEquals(msg1.hashCode(), msg2.hashCode());
        }
}
