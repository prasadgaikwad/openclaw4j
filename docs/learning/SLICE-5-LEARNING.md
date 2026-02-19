# Slice 5 — Learning Guide: RAG Knowledge Base

> **What you built:** A Retrieval-Augmented Generation (RAG) system that allows the agent to semantically search and recall information from past channel history.
> **Concepts covered:** Vector Databases (PGVector), Document Embeddings, Spring AI `VectorStore`, and Scheduled Indexing pipelines.

---

## 1. Conceptual Model: RAG Pipeline

| Component | Implementation | Purpose | Storage |
|-------|----------------|---------|-----------|
| **Indexer** | `ChannelIndexer` | Periodic fetching and tokenizing of Slack history | N/A |
| **Vector Store** | `PGVector` (PostgreSQL) | Stores document embeddings for semantic search | Persistent (DB) |
| **Service** | `VectorStoreRAGService` | Provides similarity search and indexing interface | N/A |
| **Tool** | `RAGTool` | Allows the agent to explicitly search the knowledge base | N/A |

---

## 2. Vector Stores & Spring AI

### 2.1 Why PGVector?
We chose **PGVector** because it adds vector similarity search capabilities directly to PostgreSQL. Since we were already using Postgres for other data, this minimized infrastructure complexity.
- **Why it matters:** It lets us store both "normal" relational data and "AI" vector data in the same place.

### 2.2 The VectorStore Abstraction
Spring AI provides a `VectorStore` interface that abstracts away whether you're using PGVector, Pinecone, or Chroma.

**Where in the code:** `VectorStoreRAGService`

```java
// Spring AI allows provider-agnostic similarity search
List<Document> result = vectorStore.similaritySearch(
    SearchRequest.builder()
        .query(query)
        .topK(5)
        .similarityThreshold(0.7)
        .build()
);
```

---

## 3. The Indexing Pipeline

### 3.1 Automated Indexing
Instead of manual uploads, Slice 5 introduces a background worker that "crawls" your slack history.

**Where in the code:** `ChannelIndexer`

```java
@Scheduled(fixedRateString = "${openclaw4j.rag.indexing-interval:PT30M}")
public void run() {
    // Fetches history and indexes documents every 30 minutes
}
```

### 3.2 Metadata Tagging
When we index a message, we don't just store the text. We attach metadata like `channelId`, `userId`, and `timestamp`.
- **Reason:** This allows for future filtering (e.g., "only search in #general") and gives the agent context about *when* something was said.

---

## 4. Integration with the Agent Core

RAG isn't just a tool; it's part of how the agent builds its "awareness":
1.  **Inbound Message:** User asks a question.
2.  **Semantic Search:** `AgentService` queries the `RAGService` for relevant history.
3.  **Context Enrichment:** The retrieved documents are added to the `AgentContext`.
4.  **Prompting:** `AgentPlanner` appends these documents to the System Message under a "Relevant Knowledge" header.

---

## 5. Troubleshooting

### 5.1 Dependency Resolution
Spring AI starters are evolving rapidly. We found that `spring-ai-starter-vector-store-pgvector` is the preferred way to get the necessary PGVector client beans autoconfigured in modern Spring Boot versions.

### 5.2 Context Load in Tests
Because RAG requires a live database, standard `@SpringBootTest` context loads can fail in environments without Postgres (like GitHub Actions).
- **Solution:** Use `@MockBean` for `VectorStore` and `DataSource` in your application smoke tests to ensure they remain fast and environment-independent.

---

## 6. Exercises

1.  **Adjust Thresholds:** Change the `similarityThreshold` in `VectorStoreRAGService` to 0.5. How does this affect the relevance of retrieved documents?
2.  **Add Filtering:** Modify the `RAGTool` to accept an optional `channelId` parameter and use a metadata filter in the `SearchRequest`.
3.  **Inspect the Tables:** Connect to your Postgres database and run `\d` to see the `vector_store` table created by Spring AI. Inspect the `embedding` column (which should be of type `vector`).
