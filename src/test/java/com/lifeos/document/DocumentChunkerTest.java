package com.lifeos.document;

import com.lifeos.document.chunking.DocumentChunk;
import com.lifeos.document.chunking.DocumentChunker;
import com.lifeos.document.extractor.PageContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChunkerTest {

    private DocumentChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new DocumentChunker();
    }

    @Test
    @DisplayName("Should return empty list for null or empty pages")
    void shouldReturnEmptyListForEmptyPages() {
        UUID docId = UUID.randomUUID();
        assertThat(chunker.chunk(docId, 1, "test.pdf", null)).isEmpty();
        assertThat(chunker.chunk(docId, 1, "test.pdf", List.of())).isEmpty();
    }

    @Test
    @DisplayName("Should chunk text and preserve page numbers and breadcrumbs")
    void shouldChunkTextAndPreservePageNumbers() {
        UUID docId = UUID.randomUUID();
        String page1Text = "SECTION 1: Policy Overview\n" +
                "This policy covers emergency medical treatments and hospital stays. " +
                "Coverage applies worldwide for trips up to 90 consecutive days. " +
                "Pre-existing conditions require written underwriter approval.";

        String page2Text = "SECTION 2: Exclusions and Limitations\n" +
                "War and nuclear contamination are strictly excluded from coverage. " +
                "Hazardous sports require a supplementary accidental damage rider.";

        List<PageContent> pages = List.of(
                new PageContent(1, page1Text),
                new PageContent(2, page2Text)
        );

        List<DocumentChunk> chunks = chunker.chunk(docId, 1, "Insurance_Policy.pdf", pages);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);

        DocumentChunk chunk1 = chunks.get(0);
        assertThat(chunk1.getPageNumber()).isEqualTo(1);
        assertThat(chunk1.getSectionTitle()).containsIgnoringCase("Policy Overview");
        assertThat(chunk1.getContent()).contains("[Document: Insurance_Policy.pdf");
        assertThat(chunk1.getContent()).contains("Section: Policy Overview");
        assertThat(chunk1.getContent()).contains("Page: 1");

        DocumentChunk chunk2 = chunks.get(1);
        assertThat(chunk2.getPageNumber()).isEqualTo(2);
        assertThat(chunk2.getSectionTitle()).containsIgnoringCase("Exclusions and Limitations");
    }

    @Test
    @DisplayName("Should generate deterministic chunk UUIDs for same doc, version, and index")
    void shouldGenerateDeterministicChunkUuids() {
        UUID docId = UUID.randomUUID();
        List<PageContent> pages = List.of(
                new PageContent(1, "LifeOS deterministic chunking test sentence. Another sentence here.")
        );

        List<DocumentChunk> run1 = chunker.chunk(docId, 1, "doc.pdf", pages);
        List<DocumentChunk> run2 = chunker.chunk(docId, 1, "doc.pdf", pages);

        assertThat(run1).hasSize(run2.size());
        for (int i = 0; i < run1.size(); i++) {
            assertThat(run1.get(i).getId()).isEqualTo(run2.get(i).getId());
            assertThat(run1.get(i).getChunkIndex()).isEqualTo(run2.get(i).getChunkIndex());
        }
    }
}
