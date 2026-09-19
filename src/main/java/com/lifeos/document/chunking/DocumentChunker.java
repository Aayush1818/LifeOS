package com.lifeos.document.chunking;

import com.lifeos.document.extractor.PageContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic, layout-aware sliding window chunker for LifeOS documents.
 * Preserves page boundaries, headings, paragraph integrity, and sentence structure.
 */
@Slf4j
@Component
public class DocumentChunker {

    public static final int DEFAULT_TARGET_TOKENS = 500; // ~1800-2000 chars
    public static final int DEFAULT_OVERLAP_TOKENS = 100; // ~360-400 chars
    public static final int CHARS_PER_TOKEN = 4;
    public static final int MIN_CHUNK_CHARS = 50;

    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "(?m)^(?:#{1,4}\\s+|SECTION\\s+\\d+|ARTICLE\\s+[IVXLCDM\\d]+|[A-Z][A-Z0-9\\s_-]{3,35}:)\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Splits extracted document pages into deterministic, context-enriched chunks.
     *
     * @param documentId source document ID
     * @param documentVersion document version
     * @param originalFilename source filename for breadcrumb context
     * @param pages list of extracted pages
     * @return ordered list of DocumentChunks
     */
    public List<DocumentChunk> chunk(UUID documentId, int documentVersion, String originalFilename, List<PageContent> pages) {
        if (pages == null || pages.isEmpty()) {
            return Collections.emptyList();
        }

        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;
        String currentSection = "General";

        for (PageContent page : pages) {
            String pageText = page.text() != null ? page.text().trim() : "";
            if (pageText.isEmpty()) {
                continue;
            }

            int pageNumber = page.pageNumber() > 0 ? page.pageNumber() : 1;

            // Detect section heading updates on this page
            Matcher headingMatcher = HEADING_PATTERN.matcher(pageText);
            if (headingMatcher.find()) {
                String headingGroup = headingMatcher.group(1);
                currentSection = headingGroup.replaceFirst("^[:\\s-]+", "").trim();
            }

            // Split page text into sentences
            List<String> sentences = splitIntoSentences(pageText);
            if (sentences.isEmpty()) {
                continue;
            }

            int targetChars = DEFAULT_TARGET_TOKENS * CHARS_PER_TOKEN;
            int overlapChars = DEFAULT_OVERLAP_TOKENS * CHARS_PER_TOKEN;

            int startIdx = 0;
            while (startIdx < sentences.size()) {
                StringBuilder chunkBuilder = new StringBuilder();
                int currentChars = 0;
                int endIdx = startIdx;

                while (endIdx < sentences.size()) {
                    String sentence = sentences.get(endIdx);
                    if (currentChars + sentence.length() > targetChars && currentChars >= MIN_CHUNK_CHARS) {
                        break;
                    }
                    if (!chunkBuilder.isEmpty()) {
                        chunkBuilder.append(" ");
                    }
                    chunkBuilder.append(sentence);
                    currentChars = chunkBuilder.length();
                    endIdx++;
                }

                String rawContent = chunkBuilder.toString().trim();
                if (!rawContent.isEmpty()) {
                    // Inject heading breadcrumb context
                    String breadcrumb = String.format("[Document: %s | Section: %s | Page: %d]\n",
                            sanitizeBreadcrumb(originalFilename), sanitizeBreadcrumb(currentSection), pageNumber);
                    String enrichedContent = breadcrumb + rawContent;

                    UUID chunkId = UUID.nameUUIDFromBytes(
                            (documentId + ":" + documentVersion + ":" + chunkIndex).getBytes(StandardCharsets.UTF_8)
                    );

                    int estimatedTokens = Math.max(1, enrichedContent.length() / CHARS_PER_TOKEN);

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("documentId", documentId.toString());
                    metadata.put("documentVersion", documentVersion);
                    metadata.put("pageNumber", pageNumber);
                    metadata.put("sectionTitle", currentSection);
                    metadata.put("filename", originalFilename);

                    chunks.add(DocumentChunk.builder()
                            .id(chunkId)
                            .chunkIndex(chunkIndex)
                            .pageNumber(pageNumber)
                            .sectionTitle(currentSection)
                            .content(enrichedContent)
                            .tokenCount(estimatedTokens)
                            .charCount(enrichedContent.length())
                            .metadata(metadata)
                            .build());

                    chunkIndex++;
                }

                // Advance window with overlap
                if (endIdx >= sentences.size()) {
                    break;
                }

                // Find next startIdx that provides approximately overlapChars
                int backtrackChars = 0;
                int nextStart = endIdx;
                while (nextStart > startIdx && backtrackChars < overlapChars) {
                    nextStart--;
                    backtrackChars += sentences.get(nextStart).length();
                }

                if (nextStart == startIdx) {
                    startIdx = endIdx; // Ensure forward progress
                } else {
                    startIdx = nextStart;
                }
            }
        }

        log.info("Document [{}] v{} partitioned into {} chunks across {} pages",
                documentId, documentVersion, chunks.size(), pages.size());
        return chunks;
    }

    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
        iterator.setText(text);

        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String sentence = text.substring(start, end).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }

        // Fallback for unbroken text
        if (sentences.isEmpty() && !text.isBlank()) {
            sentences.add(text.trim());
        }
        return sentences;
    }

    private String sanitizeBreadcrumb(String str) {
        if (str == null || str.isBlank()) {
            return "General";
        }
        return str.replace("\n", " ").replace("\r", " ").trim();
    }
}
