package com.lifeos.rag.retrieval;

import com.lifeos.rag.retrieval.model.RetrievalCandidate;
import com.lifeos.rag.retrieval.processor.QueryProcessor;
import com.lifeos.rag.retrieval.processor.QueryProcessor.ProcessedQuery;
import com.lifeos.rag.retrieval.rerank.DeterministicCrossSignalReranker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicCrossSignalRerankerTest {

    private DeterministicCrossSignalReranker reranker;
    private QueryProcessor queryProcessor;

    @BeforeEach
    void setUp() {
        reranker = new DeterministicCrossSignalReranker();
        queryProcessor = new QueryProcessor();
    }

    @Test
    @DisplayName("rerank - boosts candidate with exact phrase match")
    void testExactPhraseBoost() {
        ProcessedQuery query = queryProcessor.process("Find \"outpatient deductible\" amount");

        RetrievalCandidate c1 = RetrievalCandidate.builder()
                .chunkId(UUID.randomUUID())
                .content("General health terms and conditions.")
                .fusedScore(0.80)
                .documentVersion(1)
                .build();

        RetrievalCandidate c2 = RetrievalCandidate.builder()
                .chunkId(UUID.randomUUID())
                .content("In-network outpatient deductible is $250 per policy year.")
                .fusedScore(0.75) // Lower initial fused score
                .documentVersion(1)
                .build();

        List<RetrievalCandidate> reranked = reranker.rerank(query, List.of(c1, c2), 5);

        // c2 had lower base score but exact phrase match (1.25x boost), so it should rank first
        assertThat(reranked.get(0).getChunkId()).isEqualTo(c2.getChunkId());
        assertThat(reranked.get(0).getRerankScore()).isGreaterThan(reranked.get(1).getRerankScore());
    }

    @Test
    @DisplayName("rerank - boosts candidate with section title match")
    void testSectionTitleMatchBoost() {
        ProcessedQuery query = queryProcessor.process("dental coverage");

        RetrievalCandidate c1 = RetrievalCandidate.builder()
                .chunkId(UUID.randomUUID())
                .documentTitle("Policy 2026")
                .sectionTitle("General Provisions")
                .content("Coverage details for miscellaneous services.")
                .fusedScore(0.60)
                .documentVersion(1)
                .build();

        RetrievalCandidate c2 = RetrievalCandidate.builder()
                .chunkId(UUID.randomUUID())
                .documentTitle("Policy 2026")
                .sectionTitle("Dental Coverage & Vision")
                .content("Routine cleaning covered at 100%.")
                .fusedScore(0.60)
                .documentVersion(1)
                .build();

        List<RetrievalCandidate> reranked = reranker.rerank(query, List.of(c1, c2), 5);

        assertThat(reranked.get(0).getChunkId()).isEqualTo(c2.getChunkId());
        assertThat(reranked.get(0).getRerankScore()).isGreaterThan(reranked.get(1).getRerankScore());
    }

    @Test
    @DisplayName("rerank - respects topN limit and clamps score to 1.0")
    void testTopNLimitAndClamping() {
        ProcessedQuery query = queryProcessor.process("test query");

        List<RetrievalCandidate> candidates = List.of(
                RetrievalCandidate.builder().chunkId(UUID.randomUUID()).content("test query in content").fusedScore(0.99).documentVersion(1).build(),
                RetrievalCandidate.builder().chunkId(UUID.randomUUID()).content("other content").fusedScore(0.70).documentVersion(1).build(),
                RetrievalCandidate.builder().chunkId(UUID.randomUUID()).content("more content").fusedScore(0.60).documentVersion(1).build()
        );

        List<RetrievalCandidate> reranked = reranker.rerank(query, candidates, 2);

        assertThat(reranked).hasSize(2);
        assertThat(reranked.get(0).getRerankScore()).isLessThanOrEqualTo(1.0);
    }
}
