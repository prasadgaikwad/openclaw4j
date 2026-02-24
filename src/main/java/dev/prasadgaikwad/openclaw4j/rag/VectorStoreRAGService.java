package dev.prasadgaikwad.openclaw4j.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of RAGService using Spring AI's VectorStore.
 */
@Service
public class VectorStoreRAGService implements RAGService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreRAGService.class);
    private final VectorStore vectorStore;

    public VectorStoreRAGService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<Document> findRelevantDocuments(String query) {
        log.debug("Querying vector store for: {}", query);
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(5)
                        .similarityThreshold(0.7)
                        .build());
    }

    @Override
    public void indexDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        log.info("Indexing {} documents into vector store", documents.size());

        TokenTextSplitter textSplitter = new TokenTextSplitter(256, 128, 5, 10000, true);
        List<Document> splitDocuments = textSplitter.apply(documents);

        log.info("Split into {} chunks for vector store", splitDocuments.size());
        vectorStore.add(splitDocuments);
    }
}
