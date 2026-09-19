package com.lifeos.document.extractor;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
public class ExtractionResult {

    private final String detectedMimeType;
    private final String extractedText;
    private final Integer pageCount;
    private final String title;
    private final String author;
    private final OffsetDateTime creationDate;
    @Builder.Default
    private final Map<String, Object> metadata = new HashMap<>();
    private final boolean success;
    private final String errorMessage;

    public static ExtractionResult success(String mimeType, String text, Integer pageCount,
                                          String title, String author, OffsetDateTime creationDate,
                                          Map<String, Object> metadata) {
        return ExtractionResult.builder()
                .detectedMimeType(mimeType)
                .extractedText(text)
                .pageCount(pageCount)
                .title(title)
                .author(author)
                .creationDate(creationDate)
                .metadata(metadata != null ? metadata : new HashMap<>())
                .success(true)
                .build();
    }

    public static ExtractionResult failure(String mimeType, String errorMessage, Map<String, Object> partialMetadata) {
        return ExtractionResult.builder()
                .detectedMimeType(mimeType)
                .extractedText("")
                .metadata(partialMetadata != null ? partialMetadata : new HashMap<>())
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
