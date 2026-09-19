package com.lifeos.rag.retrieval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level response contract returned by the Hybrid RAG Retrieval Engine.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResponse {

    private String query;

    private RetrievalMode retrievalMode;

    /**
     * Flag indicating whether high-confidence matching context was found.
     * When false, results list is empty and noContextReason is populated,
     * signaling to downstream LLMs to avoid hallucinating.
     */
    private boolean hasRelevantContext;

    private String noContextReason;

    @Builder.Default
    private List<RetrievedChunkDto> results = new ArrayList<>();

    @Builder.Default
    private List<RetrievalCitationDto> citations = new ArrayList<>();

    private RetrievalMetadataDto metadata;
}
