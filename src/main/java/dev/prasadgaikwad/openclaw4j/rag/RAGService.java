package dev.prasadgaikwad.openclaw4j.rag;

import org.springframework.ai.document.Document;
import java.util.List;

/**
 * Service for semantic retrieval from the agent's knowledge base.
 */
public interface RAGService {

    /**
     * Finds documents relevant to the given query.
     * 
     * @param query the search query
     * @return a list of relevant documents
     */
    List<Document> findRelevantDocuments(String query);

    /**
     * Indexes a list of documents into the vector store.
     * 
     * @param documents documents to index
     */
    void indexDocuments(List<Document> documents);
}
