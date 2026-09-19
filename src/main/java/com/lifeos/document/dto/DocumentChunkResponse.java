package com.lifeos.document.dto;

import com.lifeos.document.entity.DocumentChunkEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class DocumentChunkResponse {

    private final UUID id;
    private final UUID documentId;
    private final Integer documentVersion;
    private final Integer chunkIndex;
    private final String content;
    private final Integer pageNumber;
    private final String sectionTitle;
    private final Integer tokenCount;
    private final Integer charCount;
    private final Boolean isActive;
    private final String embeddingModel;
    private final Map<String, Object> metadata;
    private final OffsetDateTime createdAt;

    public static DocumentChunkResponse fromEntity(DocumentChunkEntity entity) {
        return DocumentChunkResponse.builder()
                .id(entity.getId())
                .documentId(entity.getDocument() != null ? entity.getDocument().getId() : null)
                .documentVersion(entity.getDocumentVersion())
                .chunkIndex(entity.getChunkIndex())
                .content(entity.getContent())
                .pageNumber(entity.getPageNumber())
                .sectionTitle(entity.getSectionTitle())
                .tokenCount(entity.getTokenCount())
                .charCount(entity.getCharCount())
                .isActive(entity.getIsActive())
                .embeddingModel(entity.getEmbeddingModel())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
