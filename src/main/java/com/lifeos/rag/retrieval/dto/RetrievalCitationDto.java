package com.lifeos.rag.retrieval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Structured provenance citation allowing downstream Phase 13 LLM assistant
 * to produce grounded answers with verified footnote citations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalCitationDto {

    private UUID documentId;

    private Integer documentVersion;

    private UUID chunkId;

    private Integer chunkIndex;

    private String documentTitle;

    private Integer pageNumber;

    private String sectionTitle;

    /**
     * Standard human-readable citation string:
     * e.g., "[Health Insurance Policy.pdf, Page 2, Section: Inpatient Benefits (v1, Chunk #3)]"
     */
    private String sourceCitation;

    private Double relevanceScore;

    private RetrievalMatchSource matchSource;
}
