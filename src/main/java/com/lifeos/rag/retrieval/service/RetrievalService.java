package com.lifeos.rag.retrieval.service;

import com.lifeos.rag.retrieval.dto.RetrievalRequest;
import com.lifeos.rag.retrieval.dto.RetrievalResponse;

import java.util.UUID;

/**
 * Core service contract for Hybrid RAG Retrieval.
 * Consumed internally by domain services and by the future Phase 13 LLM assistant.
 */
public interface RetrievalService {

    /**
     * Executes scoped hybrid retrieval for the authenticated user.
     *
     * @param userId  authenticated tenant user ID
     * @param request retrieval parameters, mode, filters, and limits
     * @return grounded retrieval context, citations, and observability telemetry
     */
    RetrievalResponse retrieve(UUID userId, RetrievalRequest request);
}
