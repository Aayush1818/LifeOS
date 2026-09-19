package com.lifeos.rag.retrieval.model;

import com.lifeos.rag.retrieval.dto.RetrievalMatchSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Internal stateful representation of a retrieved candidate passing through
 * the fusion, deduplication, reranking, and thresholding pipeline stages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalCandidate {

    private UUID chunkId;

    private UUID documentId;

    private Integer documentVersion;

    private Integer chunkIndex;

    private UUID userId;

    private String documentTitle;

    private String originalFilename;

    private String documentCategory;

    private String documentType;

    private UUID dependentId;

    private String content;

    private String snippet;

    private Integer pageNumber;

    private String sectionTitle;

    private Integer tokenCount;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    private Double lexicalScore;

    private Integer lexicalRank;

    private Double vectorScore;

    private Integer vectorRank;

    private Double fusedScore;

    private Double rerankScore;

    @Builder.Default
    private RetrievalMatchSource matchSource = RetrievalMatchSource.LEXICAL_ONLY;
}
