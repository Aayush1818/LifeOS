package com.lifeos.rag.retrieval;

import com.lifeos.rag.retrieval.processor.QueryProcessor;
import com.lifeos.rag.retrieval.processor.QueryProcessor.ProcessedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryProcessorTest {

    private QueryProcessor queryProcessor;

    @BeforeEach
    void setUp() {
        queryProcessor = new QueryProcessor();
    }

    @Test
    @DisplayName("process - normalizes whitespace and removes null bytes")
    void testNormalizeWhitespaceAndNullBytes() {
        String input = "  What   is\u0000 the \r\n deductible for \t inpatient    care?  ";
        ProcessedQuery result = queryProcessor.process(input);

        assertThat(result.normalizedQuery()).isEqualTo("What is the deductible for inpatient care?");
    }

    @Test
    @DisplayName("process - throws exception on blank or whitespace-only query")
    void testBlankQueryThrowsException() {
        assertThatThrownBy(() -> queryProcessor.process("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");

        assertThatThrownBy(() -> queryProcessor.process("\u0000\t\n"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("process - extracts exact quoted phrases")
    void testExtractQuotedPhrases() {
        String input = "Find policy with \"Section 4.2\" and \"Inpatient Care\" coverage";
        ProcessedQuery result = queryProcessor.process(input);

        assertThat(result.exactPhrases()).containsExactly("section 4.2", "inpatient care");
    }

    @Test
    @DisplayName("process - extracts alphanumeric identifiers")
    void testExtractIdentifiers() {
        String input = "Check status of invoice INV-2026-001 and claim CLM-99881";
        ProcessedQuery result = queryProcessor.process(input);

        assertThat(result.identifiers()).contains("inv-2026-001", "clm-99881");
    }

    @Test
    @DisplayName("process - clamps query exceeding 1000 characters")
    void testClampLongQuery() {
        String longInput = "a ".repeat(600); // 1200 chars
        ProcessedQuery result = queryProcessor.process(longInput);

        assertThat(result.normalizedQuery().length()).isLessThanOrEqualTo(1000);
    }
}
