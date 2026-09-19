package com.lifeos.document.chunking;

import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a structured, layout-aware document chunk ready for vector embedding and storage.
 */
@Getter
@Builder
public class DocumentChunk {

    private final UUID id;
    private final int chunkIndex;
    private final int pageNumber;
    private final String sectionTitle;
    private final String content;
    private final int tokenCount;
    private final int charCount;

    @Builder.Default
    private final Map<String, Object> metadata = new HashMap<>();
}
