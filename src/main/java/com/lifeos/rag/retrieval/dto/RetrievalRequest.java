package com.lifeos.rag.retrieval.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound request contract for Hybrid RAG Retrieval.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalRequest {

    /**
     * User search query or question. Must not be blank.
     */
    @NotBlank(message = "Query text must not be blank")
    @Size(max = 1000, message = "Query text must not exceed 1000 characters")
    private String query;

    /**
     * Retrieval mode (HYBRID, LEXICAL, SEMANTIC). Defaults to HYBRID.
     */
    @Builder.Default
    private RetrievalMode mode = RetrievalMode.HYBRID;

    /**
     * Optional structured metadata filters.
     */
    private RetrievalFilters filters;

    /**
     * Maximum number of top chunks to return in the final retrieved context.
     * Defaults to 5. Allowed range: 1 to 50.
     */
    @Builder.Default
    @Min(value = 1, message = "topK must be at least 1")
    @Max(value = 50, message = "topK must not exceed 50")
    private int topK = 5;

    /**
     * Optional minimum relevance threshold to filter out low-confidence matches.
     * If null, the system default configured in application properties is applied.
     */
    private Double minRelevanceScore;

    /**
     * Whether to include the full chunk text in the response. Defaults to true.
     */
    @Builder.Default
    private boolean includeContent = true;
}
