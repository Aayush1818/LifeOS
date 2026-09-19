package com.lifeos.rag.retrieval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Diagnostics and performance telemetry for the retrieval pipeline execution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalMetadataDto {

    private int totalCandidatesFound;

    private int lexicalCandidatesCount;

    private int vectorCandidatesCount;

    private long executionTimeMs;

    private long lexicalLatencyMs;

    private long vectorLatencyMs;

    private long fusionLatencyMs;

    private long rerankLatencyMs;

    /**
     * True if the query encountered a partial failure (e.g. embedding failure)
     * and automatically degraded to a fallback mode (e.g. lexical only).
     */
    private boolean degraded;

    private String degradationReason;
}
