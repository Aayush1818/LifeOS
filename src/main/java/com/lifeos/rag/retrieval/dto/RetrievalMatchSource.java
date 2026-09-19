package com.lifeos.rag.retrieval.dto;

/**
 * Indicates which retrieval system(s) surfaced the matching chunk.
 */
public enum RetrievalMatchSource {
    /**
     * Chunk was retrieved exclusively by PostgreSQL full-text search.
     */
    LEXICAL_ONLY,

    /**
     * Chunk was retrieved exclusively by pgvector semantic vector search.
     */
    SEMANTIC_ONLY,

    /**
     * Chunk appeared in both lexical and semantic retrieval candidate sets.
     */
    HYBRID_BOTH
}
