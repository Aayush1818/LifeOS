package com.lifeos.document.dto;

import com.lifeos.document.entity.DocumentEntity;
import com.lifeos.document.entity.IngestionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class DocumentIngestionStatusResponse {

    private final UUID documentId;
    private final IngestionStatus ingestionStatus;
    private final Integer version;
    private final Integer chunkCount;
    private final Integer pageCount;
    private final String embeddingModel;
    private final OffsetDateTime ingestedAt;
    private final String extractionError;
    private final Map<String, Object> metadata;

    public static DocumentIngestionStatusResponse fromEntity(DocumentEntity entity) {
        Integer pageCount = null;
        if (entity.getMetadata() != null && entity.getMetadata().containsKey("Page-Count")) {
            try {
                pageCount = Integer.parseInt(entity.getMetadata().get("Page-Count").toString());
            } catch (Exception ignored) {}
        }
        return DocumentIngestionStatusResponse.builder()
                .documentId(entity.getId())
                .ingestionStatus(entity.getIngestionStatus())
                .version(entity.getVersion())
                .chunkCount(entity.getChunkCount() != null ? entity.getChunkCount() : 0)
                .pageCount(pageCount)
                .embeddingModel(entity.getEmbeddingModel())
                .ingestedAt(entity.getIngestedAt())
                .extractionError(entity.getExtractionError())
                .metadata(entity.getMetadata())
                .build();
    }
}
