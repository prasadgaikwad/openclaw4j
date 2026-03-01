package dev.prasadgaikwad.openclaw4j.tool.search;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for the Search tool.
 *
 * @param apiKey  The API key for Tavily Search
 * @param enabled Whether the search tool is enabled
 */
@ConfigurationProperties(prefix = "openclaw4j.tools.search")
public record SearchProperties(
        @DefaultValue("false") boolean enabled,
        String apiKey,
        @DefaultValue("https://api.tavily.com") String baseUrl,
        @DefaultValue("/search") String apiPath,
        @DefaultValue("basic") String searchDepth,
        @DefaultValue("true") boolean includeAnswer,
        @DefaultValue("5") Integer maxResults) {
}
