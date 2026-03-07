package dev.prasadgaikwad.openclaw4j.channel.slack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlackFormatterTest {

    @ParameterizedTest(name = "Markdown \"{0}\" should convert to \"{1}\"")
    @CsvSource({
            "'**bold text**', '*bold text*'",
            "'__bold text__', '*bold text*'",
            "'*italic text*', '_italic text_'",
            "'[Google](https://google.com)', '<https://google.com|Google>'",
            "'### Header', '*Header*'",
            "'# Main Title', '*Main Title*'",
            "'Mixed **bold** and *italic*', 'Mixed *bold* and _italic_'",
            "'[Link](url) with **bold**', '<url|Link> with *bold*'",
            "'**multiline\nbold**', '*multiline\nbold*'",
            "'### Header with space  ', '*Header with space*'",
            "'*multiline\nitalic*', '_multiline\nitalic_'",
    })
    @DisplayName("Should correctly format various Markdown elements to Slack mrkdwn")
    void format_shouldConvertMarkdownToMrkdwn(String input, String expected) {
        assertEquals(expected, SlackFormatter.format(input));
    }

    @Test
    @DisplayName("Should handle null or empty input")
    void format_shouldHandleNullOrEmpty() {
        assertEquals(null, SlackFormatter.format(null));
        assertEquals("", SlackFormatter.format(""));
    }

    @Test
    @DisplayName("Should handle text without Markdown")
    void format_shouldHandlePlainText() {
        String plainText = "Just some plain text without any special formatting.";
        assertEquals(plainText, SlackFormatter.format(plainText));
    }
}
