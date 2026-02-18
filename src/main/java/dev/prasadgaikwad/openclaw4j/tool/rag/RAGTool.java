package dev.prasadgaikwad.openclaw4j.tool.rag;

import dev.prasadgaikwad.openclaw4j.rag.RAGService;
import dev.prasadgaikwad.openclaw4j.tool.AITool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Tool that allows the agent to search indexed channel history and
 * documentation.
 */
@Component
public class RAGTool implements AITool {

    private static final Logger log = LoggerFactory.getLogger(RAGTool.class);
    private final RAGService ragService;

    public RAGTool(RAGService ragService) {
        this.ragService = ragService;
    }

    /**
     * Searches the knowledge base for information matching the query.
     * 
     * @param query the search query
     * @return a summary of relevant documents found
     */
    @Tool(description = "Searches the agent's knowledge base for information from past channel discussions and documentation.")
    public String searchKnowledgeBase(String query) {
        log.info("Agent searching knowledge base for: {}", query);
        var docs = ragService.findRelevantDocuments(query);

        if (docs.isEmpty()) {
            return "No relevant information found in the knowledge base for: " + query;
        }

        return docs.stream()
                .map(doc -> {
                    String timestamp = (String) doc.getMetadata().getOrDefault("timestamp", "unknown");
                    return String.format("[%s] %s", timestamp, doc.getText());
                })
                .collect(Collectors.joining("\n---\n"));
    }
}
