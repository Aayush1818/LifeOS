package com.lifeos.ai;

import com.lifeos.ai.dto.AssistantCitationDto;
import com.lifeos.ai.grounding.AssembledContext;
import com.lifeos.ai.grounding.CitationValidator;
import com.lifeos.ai.grounding.ValidatedCitationResult;
import com.lifeos.rag.retrieval.dto.RetrievalCitationDto;
import com.lifeos.rag.retrieval.dto.RetrievedChunkDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CitationValidatorTest {

    private CitationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CitationValidator();
    }

    @Test
    @DisplayName("Should extract and validate matching inline citations [1] and [2]")
    void shouldExtractAndValidateMatchingCitations() {
        UUID docId1 = UUID.randomUUID();
        UUID docId2 = UUID.randomUUID();

        RetrievalCitationDto cit1 = RetrievalCitationDto.builder()
                .documentId(docId1)
                .documentTitle("Policy.pdf")
                .pageNumber(2)
                .sectionTitle("General")
                .sourceCitation("[Policy.pdf, Page 2]")
                .relevanceScore(0.89)
                .build();

        RetrievalCitationDto cit2 = RetrievalCitationDto.builder()
                .documentId(docId2)
                .documentTitle("Warranty.pdf")
                .pageNumber(1)
                .sectionTitle("Terms")
                .sourceCitation("[Warranty.pdf, Page 1]")
                .relevanceScore(0.85)
                .build();

        AssembledContext context = AssembledContext.builder()
                .sourceCount(2)
                .citationMap(Map.of(1, cit1, 2, cit2))
                .sourceMap(Map.of(
                        1, RetrievedChunkDto.builder().documentId(docId1).snippet("policy snippet").build(),
                        2, RetrievedChunkDto.builder().documentId(docId2).snippet("warranty snippet").build()
                ))
                .build();

        String rawAnswer = "Your policy covers inpatient care [1]. Also hardware is under warranty [2].";
        ValidatedCitationResult result = validator.validate(rawAnswer, context);

        assertThat(result.getValidCitations()).hasSize(2);
        assertThat(result.getInvalidCitationsRemoved()).isEqualTo(0);
        assertThat(result.getSanitizedContent()).isEqualTo(rawAnswer);

        AssistantCitationDto c1 = result.getValidCitations().get(0);
        assertThat(c1.getCitationIndex()).isEqualTo(1);
        assertThat(c1.getDocumentTitle()).isEqualTo("Policy.pdf");

        AssistantCitationDto c2 = result.getValidCitations().get(1);
        assertThat(c2.getCitationIndex()).isEqualTo(2);
        assertThat(c2.getDocumentTitle()).isEqualTo("Warranty.pdf");
    }

    @Test
    @DisplayName("Should strip hallucinated citation [99] and preserve valid citation [1]")
    void shouldStripHallucinatedCitation() {
        UUID docId1 = UUID.randomUUID();
        RetrievalCitationDto cit1 = RetrievalCitationDto.builder()
                .documentId(docId1)
                .documentTitle("Policy.pdf")
                .pageNumber(2)
                .build();

        AssembledContext context = AssembledContext.builder()
                .sourceCount(1)
                .citationMap(Map.of(1, cit1))
                .sourceMap(Map.of(1, RetrievedChunkDto.builder().documentId(docId1).build()))
                .build();

        String rawAnswer = "Your coverage is active [1], but other clauses apply [99].";
        ValidatedCitationResult result = validator.validate(rawAnswer, context);

        assertThat(result.getValidCitations()).hasSize(1);
        assertThat(result.getInvalidCitationsRemoved()).isEqualTo(1);
        assertThat(result.getSanitizedContent()).doesNotContain("[99]");
        assertThat(result.getSanitizedContent()).contains("[1]");
    }

    @Test
    @DisplayName("Should strip all citations if context was empty")
    void shouldStripAllCitationsIfContextWasEmpty() {
        AssembledContext context = AssembledContext.builder().sourceCount(0).build();
        String rawAnswer = "I think you have coverage [1].";

        ValidatedCitationResult result = validator.validate(rawAnswer, context);

        assertThat(result.getValidCitations()).isEmpty();
        assertThat(result.getSanitizedContent()).isEqualTo("I think you have coverage .");
    }
}
