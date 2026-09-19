package com.lifeos.rag.retrieval.dto;

import com.lifeos.document.entity.DocumentCategory;
import com.lifeos.document.entity.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Structured metadata filters for scoped hybrid RAG retrieval.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalFilters {

    /**
     * Optional filter by document high-level category.
     */
    private DocumentCategory category;

    /**
     * Optional filter by specific document type.
     */
    private DocumentType documentType;

    /**
     * Optional filter by linked entity type (e.g. "ASSET", "INVOICE", "TRIP").
     */
    private String entityType;

    /**
     * Optional filter by linked entity ID.
     */
    private UUID entityId;

    /**
     * Optional filter by dependent ID.
     */
    private UUID dependentId;

    /**
     * Optional filter to restrict retrieval to a single document ID.
     */
    private UUID documentId;

    /**
     * Optional filter by document version. If null, active version chunks are retrieved.
     */
    private Integer documentVersion;

    /**
     * Optional filter for documents issued on or after this date.
     */
    private LocalDate startDate;

    /**
     * Optional filter for documents issued on or before this date.
     */
    private LocalDate endDate;

    /**
     * Optional substring filter on section title.
     */
    private String section;

    /**
     * Optional filter by specific page number.
     */
    private Integer pageNumber;

    /**
     * Optional filter by document MIME type.
     */
    private String mimeType;

    /**
     * If true, includes chunks from older deactivated document versions.
     * Defaults to false (active chunks only).
     */
    @Builder.Default
    private Boolean includeHistorical = false;
}
