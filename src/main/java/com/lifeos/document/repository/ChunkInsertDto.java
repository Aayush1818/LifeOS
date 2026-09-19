package com.lifeos.document.repository;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class ChunkInsertDto {
    private final UUID id;
    private final UUID documentId;
    private final UUID userId;
    private final int documentVersion;
    private final int chunkIndex;
    private final String content;
    private final float[] embedding;
    private final int pageNumber;
    private final String sectionTitle;
    private final int tokenCount;
    private final int charCount;
    private final boolean isActive;
    private final String embeddingModel;
    private final Map<String, Object> metadata;
}
