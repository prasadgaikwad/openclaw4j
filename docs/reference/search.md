# Search Reference

OpenClaw4J uses the Tavily AI search engine to find the latest information on the internet.

## Features

-   **Deep Search**: Capabilities for a deeper or broader search of the web.
-   **AI Summarization**: Returns both a consolidated list of results and an AI-generated summary.
-   **Contextualization**: This tool is ideal for current events, news, or any information not present in the internal knowledge base.

## Available Tools

#### `search(String query)`
Performs a web search based on the query. It returns a consolidated string of the top results, including titles, URLs, and snippets. Use this whenever you're unsure about factual data that could have changed after your training cutoff.

---

## Configuration

Search properties are configured in `application.yml` or through environment variables:

-   `openclaw4j.tools.search.enabled`: Global toggle to enable the search tool.
-   `openclaw4j.tools.search.api-key`: Your Tavily API key.
-   `openclaw4j.tools.search.search-depth`: How deep to search (e.g., `basic` or `advanced`).
-   `openclaw4j.tools.search.max-results`: Number of results to return.
-   `openclaw4j.tools.search.include-answer`: Whether to include an AI-synthesized answer in the response.
