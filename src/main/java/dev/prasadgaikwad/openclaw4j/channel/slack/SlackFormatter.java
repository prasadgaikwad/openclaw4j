package dev.prasadgaikwad.openclaw4j.channel.slack;

import java.util.regex.Pattern;

/**
 * Utility class to convert standard Markdown to Slack's mrkdwn format.
 *
 * <p>
 * Slack uses a specific version of Markdown called mrkdwn. This class
 * handles common conversions from standard Markdown to mrkdwn.
 * </p>
 *
 * @author Prasad Gaikwad
 */
public final class SlackFormatter {

    // Regex for [text](url) -> <url|text>
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");

    // Regex for bold **text** or __text__ -> *text* (multiline support)
    private static final Pattern BOLD_PATTERN = Pattern.compile("(?s)(\\*\\*|__)(.*?)\\1");

    // Regex for italic *text* or _text_ -> _text_
    // Note: This is simpler because Slack uses _ for italic, same as one variety of
    // standard Markdown.
    // However, standard Markdown also uses * for italic, which Slack uses for bold.
    private static final Pattern ITALIC_STAR_PATTERN = Pattern.compile("(?s)(?<!\\*)\\*([^\\*]+)\\*(?!\\*)");

    // Regex for headers ### Header -> *Header*
    // Slack doesn't have true headers, so we bold them. Covers trailing
    // spaces/newlines
    private static final Pattern HEADER_PATTERN = Pattern.compile("(?m)^#{1,6}\\s+(.*?)\\s*$");

    private SlackFormatter() {
        // Utility class
    }

    /**
     * Formats standard Markdown text into Slack's mrkdwn format.
     *
     * @param markdown the standard Markdown text
     * @return the mrkdwn formatted text
     */
    public static String format(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return markdown;
        }

        String formatted = markdown;

        // 1. Convert links: [text](url) -> <url|text>
        formatted = LINK_PATTERN.matcher(formatted).replaceAll("<$2|$1>");

        // 2. Convert italic: *text* -> _text_
        // Handle * specifically since it's common. _ is already used by Slack for
        // italic.
        formatted = ITALIC_STAR_PATTERN.matcher(formatted).replaceAll("_$1_");

        // 3. Convert bold: **text** -> *text*
        formatted = BOLD_PATTERN.matcher(formatted).replaceAll("*$2*");

        // 4. Convert headers: ### Header -> *Header*
        formatted = HEADER_PATTERN.matcher(formatted).replaceAll("*$1*");

        return formatted;
    }
}
