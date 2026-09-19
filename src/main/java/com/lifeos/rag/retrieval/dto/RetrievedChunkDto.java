package com.lifeos.rag.retrieval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Single retrieved chunk unit returned in the retrieval response.
 * <p>
 * Strictly excludes raw vector embeddings to prevent data leakage and excessive payload size.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedChunkDto {

    private UUID chunkId;

    private UUID documentId;

    private Integer documentVersion;

    private Integer chunkIndex;

    private String documentTitle;

    private String originalFilename;

    private String category;

    private String documentType;

    private UUID dependentId;

    private Integer pageNumber;

    private String sectionTitle;

    private Integer tokenCount;

    /**
     * Chunk text including breadcrumbs. Omitted if includeContent=false was requested.
     */
    private String content;

    /**
     * Highlighted text excerpt matching query terms.
     */
    private String snippet;

    /**
     * Final combined relevance score (in [0.0, 1.0]).
     */
    private Double relevanceScore;

    /**
     * Source of match (LEXICAL_ONLY, SEMANTIC_ONLY, HYBRID_BOTH).
     */
    private RetrievalMatchSource matchSource;

    /**
     * Arbitrary chunk metadata attributes.
     */
    private Map<String, Object> metadata;
}
