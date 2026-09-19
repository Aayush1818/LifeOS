package com.lifeos.rag.retrieval.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Hybrid RAG Retrieval Engine.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "lifeos.rag.retrieval")
public class RetrievalProperties {

    /**
     * Default number of lexical candidates to retrieve from PostgreSQL FTS.
     */
    private int lexicalTopK = 20;

    /**
     * Default number of vector candidates to retrieve from pgvector HNSW.
     */
    private int vectorTopK = 20;

    /**
     * Reciprocal Rank Fusion constant (k). Standard default is 60.
     */
    private int rrfConstantK = 60;

    /**
     * Weight assigned to lexical rank in RRF fusion.
     */
    private double lexicalWeight = 1.0;

    /**
     * Weight assigned to vector rank in RRF fusion.
     */
    private double vectorWeight = 1.0;

    /**
     * Minimum vector cosine similarity threshold [0.0, 1.0] for vector candidates.
     */
    private double minSimilarityThreshold = 0.40;

    /**
     * Minimum normalized fused RRF score [0.0, 1.0] to qualify as relevant context.
     */
    private double minFusedScoreThreshold = 0.15;

    /**
     * Minimum score after reranking [0.0, 1.0] to be included in final results.
     */
    private double minRerankScoreThreshold = 0.20;

    /**
     * Maximum number of fused candidates passed to the reranking stage.
     */
    private int rerankLimit = 10;

    /**
     * Maximum aggregate characters of chunk content returned to prevent context overflow.
     */
    private int maxContextChars = 16000;
}
