package com.lifeos.rag.retrieval.rerank;

import com.lifeos.rag.retrieval.model.RetrievalCandidate;
import com.lifeos.rag.retrieval.processor.QueryProcessor.ProcessedQuery;

import java.util.List;

/**
 * Pluggable SPI for candidate chunk reranking.
 * Decouples reranking strategy (local heuristic, cross-encoder, Cohere, BGE) from retrieval orchestration.
 */
public interface Reranker {

    /**
     * Reranks candidate chunks based on deeper query-content cross-signals.
     *
     * @param query      processed query containing normalized terms and exact phrases
     * @param candidates candidates surviving the fusion stage
     * @param topN       maximum number of top candidates to return
     * @return reranked candidates with populated rerankScore
     */
    List<RetrievalCandidate> rerank(ProcessedQuery query, List<RetrievalCandidate> candidates, int topN);
}
