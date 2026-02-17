package dev.prasadgaikwad.openclaw4j.tool.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import dev.prasadgaikwad.openclaw4j.tool.AITool;

import java.util.Map;

/**
 * MCP Tool for interacting with GitHub.
 */
@Service
public class GitHubTool implements AITool {

    private static final Logger logger = LoggerFactory.getLogger(GitHubTool.class);
    private final RestTemplate restTemplate;

    @Value("${openclaw4j.tools.github.token:}")
    private String githubToken;

    @Value("${openclaw4j.tools.github.owner:}")
    private String defaultOwner;

    @Value("${openclaw4j.tools.github.repo:}")
    private String defaultRepo;

    public GitHubTool(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * Creates a new issue in a GitHub repository.
     *
     * @param title The title of the issue
     * @param body  The body of the issue (markdown)
     * @param repo  Full repository name in format 'owner/repo'. If null, uses
     *              defaults.
     * @return Confirmation message or error
     */
    @Tool(description = "Creates a new issue in a GitHub repository. Use this to track tasks, bugs, or feature requests.")
    public String createGitHubIssue(String title, String body, String repo) {
        logger.info("Creating GitHub issue: title={}, repo={}", title, repo);

        String targetRepo = (repo != null && !repo.isBlank()) ? repo : defaultOwner + "/" + defaultRepo;
        String url = String.format("https://api.github.com/repos/%s/issues", targetRepo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(githubToken);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        Map<String, String> payload = Map.of(
                "title", title,
                "body", body);

        try {
            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            Map<?, ?> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.containsKey("html_url")) {
                return "Issue created successfully. URL: " + response.get("html_url");
            }
            return "Failed to create issue. Response was empty.";
        } catch (Exception e) {
            logger.error("Error creating GitHub issue", e);
            return "Error creating issue: " + e.getMessage();
        }
    }
}
