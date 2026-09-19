package com.lifeos.rag.retrieval.dto;

/**
 * Supported retrieval modes for Phase 12 Hybrid RAG Retrieval Engine.
 */
public enum RetrievalMode {
    /**
     * Executes both PostgreSQL full-text search and pgvector semantic search,
     * fusing results via Reciprocal Rank Fusion (RRF) and deduplicating.
     */
    HYBRID,

    /**
     * Executes PostgreSQL full-text search only. Zero external embedding latency.
     * Optimal for exact identifiers, invoice numbers, policy codes, and keywords.
     */
    LEXICAL,

    /**
     * Executes pgvector semantic search only.
     * Optimal for natural language questions and conceptual similarity.
     */
    SEMANTIC
}
