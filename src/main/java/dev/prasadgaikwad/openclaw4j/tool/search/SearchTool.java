package dev.prasadgaikwad.openclaw4j.tool.search;

import java.util.Objects;

import dev.prasadgaikwad.openclaw4j.tool.AITool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A search tool that gives the agent access to the latest information via
 * Tavily AI.
 *
 * @author Prasad Gaikwad
 */
@Component
@ConditionalOnProperty(name = "openclaw4j.tools.search.enabled", havingValue = "true")
public class SearchTool implements AITool {

    private static final String SEARCH_DEPTH = "search_depth";
    private static final String INCLUDE_ANSWER = "include_answer";
    private static final String MAX_RESULTS = "max_results";
    private static final String QUERY = "query";
    private static final String API_KEY = "api_key";
    private static final Logger log = LoggerFactory.getLogger(SearchTool.class);
    private final RestClient restClient;
    private final SearchProperties properties;

    public SearchTool(@Qualifier("searchRestClient") RestClient restClient, SearchProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /**
     * Performs a web search and returns relevant results.
     *
     * @param query The search query
     * @return A consolidated string of search results and an optional AI-generated
     *         answer
     */
    @Tool(description = "Performs a web search to find the latest information on the internet. Use this for news, current events, or information not present in the internal knowledge base.")
    public String search(String query) {
        log.info("Performing web search for: {}", query);

        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            return "Error: Tavily API key is not configured.";
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    API_KEY, properties.apiKey(),
                    QUERY, query,
                    SEARCH_DEPTH, properties.searchDepth(),
                    INCLUDE_ANSWER, properties.includeAnswer(),
                    MAX_RESULTS, properties.maxResults());

            TavilyResponse response = restClient.post()
                    .uri(Objects.requireNonNull(properties.apiPath(), "Tavily API path must not be null"))
                    .body(Objects.requireNonNull(requestBody, "Request body must not be null"))
                    .retrieve()
                    .toEntity(TavilyResponse.class)
                    .getBody();

            if (response == null || response.results() == null) {
                return "The search returned no results for: " + query;
            }

            StringBuilder sb = new StringBuilder();
            if (response.answer() != null && !response.answer().isBlank()) {
                sb.append("AI Summary: ").append(response.answer()).append("\n\n");
            }

            String results = response.results().stream()
                    .map(r -> String.format("Title: %s\nURL: %s\nContent: %s", r.title(), r.url(), r.content()))
                    .collect(Collectors.joining("\n\n---\n\n"));

            sb.append("Search Results:\n").append(results);

            String searchResult = sb.toString();
            log.info("Search result for query {}: {}", query, searchResult);
            return searchResult;
        } catch (Exception e) {
            log.error("Search failed for query {}: {}", query, e.getMessage());
            return "Search failed: " + e.getMessage();
        }
    }

    /**
     * Internal records for Tavily API response mapping.
     */
    private record TavilyResponse(String answer, List<TavilyResult> results) {
    }

    private record TavilyResult(String title, String url, String content, double score) {
    }
}
