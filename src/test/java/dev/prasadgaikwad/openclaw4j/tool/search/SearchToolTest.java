package dev.prasadgaikwad.openclaw4j.tool.search;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest({ SearchTool.class, SearchConfig.class })
@TestPropertySource(properties = {
                "openclaw4j.tools.search.api-key=test-api-key",
                "openclaw4j.tools.search.enabled=true",
                "openclaw4j.tools.search.base-url=https://api.tavily.com",
                "openclaw4j.tools.search.api-path=/search",
                "openclaw4j.tools.search.search-depth=basic",
                "openclaw4j.tools.search.include-answer=true",
                "openclaw4j.tools.search.max-results=5"
})
public class SearchToolTest {

        @Autowired
        private SearchTool searchTool;

        @Autowired
        @Qualifier("searchRestClient")
        private RestClient restClient;

        @Autowired
        private MockRestServiceServer server;

        @Test
        void searchReturnsResults() {
                String query = "test query";
                String mockResponse = """
                                {
                                  "answer": "This is a summary answer",
                                  "results": [
                                    {
                                      "title": "Result 1",
                                      "url": "https://example.com/1",
                                      "content": "Content 1",
                                      "score": 0.95
                                    }
                                  ]
                                }
                                """;

                this.server.expect(requestTo("https://api.tavily.com/search"))
                                .andExpect(jsonPath("$.query").value(query))
                                .andExpect(jsonPath("$.api_key").value("test-api-key"))
                                .andExpect(jsonPath("$.search_depth").value("basic"))
                                .andExpect(jsonPath("$.include_answer").value(true))
                                .andExpect(jsonPath("$.max_results").value(5))
                                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

                String result = searchTool.search(query);

                assertThat(result).contains("AI Summary: This is a summary answer");
                assertThat(result).contains("Title: Result 1");
                assertThat(result).contains("URL: https://example.com/1");
                assertThat(result).contains("Content: Content 1");
        }

        @Test
        void searchHandlesError() {
                this.server.expect(requestTo("https://api.tavily.com/search"))
                                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                                                .withServerError());

                String result = searchTool.search("query");
                assertThat(result).startsWith("Search failed:");
        }
}
