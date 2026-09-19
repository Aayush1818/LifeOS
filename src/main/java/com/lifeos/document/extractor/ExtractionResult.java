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
    @Builder.Default
    private final java.util.List<PageContent> pages = new java.util.ArrayList<>();
    private final boolean success;
    private final String errorMessage;

    public java.util.List<PageContent> getPages() {
        if (pages != null && !pages.isEmpty()) {
            return pages;
        }
        if (extractedText != null && !extractedText.isBlank()) {
            return java.util.List.of(new PageContent(1, extractedText));
        }
        return java.util.Collections.emptyList();
    }

    public static ExtractionResult success(String mimeType, String text, Integer pageCount,
                                          String title, String author, OffsetDateTime creationDate,
                                          Map<String, Object> metadata) {
        return success(mimeType, text, pageCount, title, author, creationDate, metadata, null);
    }

    public static ExtractionResult success(String mimeType, String text, Integer pageCount,
                                          String title, String author, OffsetDateTime creationDate,
                                          Map<String, Object> metadata, java.util.List<PageContent> pages) {
        java.util.List<PageContent> pageList = pages != null ? pages : new java.util.ArrayList<>();
        if (pageList.isEmpty() && text != null && !text.isBlank()) {
            pageList.add(new PageContent(1, text));
        }
        int resolvedPageCount = pageCount != null ? pageCount : Math.max(1, pageList.size());
        return ExtractionResult.builder()
                .detectedMimeType(mimeType)
                .extractedText(text)
                .pageCount(resolvedPageCount)
                .title(title)
                .author(author)
                .creationDate(creationDate)
                .metadata(metadata != null ? metadata : new HashMap<>())
                .pages(pageList)
                .success(true)
                .build();
    }

    public static ExtractionResult failure(String mimeType, String errorMessage, Map<String, Object> partialMetadata) {
        return ExtractionResult.builder()
                .detectedMimeType(mimeType)
                .extractedText("")
                .metadata(partialMetadata != null ? partialMetadata : new HashMap<>())
                .pages(new java.util.ArrayList<>())
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
