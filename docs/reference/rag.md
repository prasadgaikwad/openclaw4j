# RAG Reference

Retrieval-Augmented Generation (RAG) is used to search indexed channel history and documentation stored in the agent's knowledge base.

## Features

-   **Semantic Retrieval**: Uses a vector store for search, performing better than keyword matches for complex queries.
-   **Contextual Sourcing**: Retrieves specific snippets of past channel discussions and documentation.
-   **Traceability**: Each document chunk is returned with a timestamp for better context.

## Available Tools

#### `searchKnowledgeBase(String query)`
Searches the user-provided knowledge base for information matching the query. This is the primary way to access information from past discussions, documentation, and technical notes that are too large for memory.

---

## Vector Store Integration

OpenClaw4J uses Spring AI's vector store abstraction, supporting multiple backends like PGVector. Documents are automatically split and indexed for enhanced relevance during retrieval.
