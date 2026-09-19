package com.lifeos.rag.retrieval;

import com.lifeos.rag.retrieval.config.RetrievalProperties;
import com.lifeos.rag.retrieval.dto.RetrievalMatchSource;
import com.lifeos.rag.retrieval.fusion.CandidateFusionEngine;
import com.lifeos.rag.retrieval.model.RetrievalCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateFusionEngineTest {

    private CandidateFusionEngine fusionEngine;
    private RetrievalProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RetrievalProperties();
        properties.setRrfConstantK(60);
        properties.setLexicalWeight(1.0);
        properties.setVectorWeight(1.0);
        fusionEngine = new CandidateFusionEngine(properties);
    }

    @Test
    @DisplayName("fuse - candidate appearing in both lexical and vector ranks higher")
    void testOverlappingCandidatesBoosted() {
        UUID sharedChunkId = UUID.randomUUID();
        UUID lexicalOnlyId = UUID.randomUUID();
        UUID vectorOnlyId = UUID.randomUUID();

        RetrievalCandidate lex1 = RetrievalCandidate.builder()
                .chunkId(sharedChunkId)
                .content("Shared Chunk Content")
                .snippet("Shared Snippet")
                .lexicalScore(0.85)
                .build();

        RetrievalCandidate lex2 = RetrievalCandidate.builder()
                .chunkId(lexicalOnlyId)
                .content("Lexical Only Content")
                .lexicalScore(0.70)
                .build();

        RetrievalCandidate vec1 = RetrievalCandidate.builder()
                .chunkId(sharedChunkId)
                .content("Shared Chunk Content")
                .vectorScore(0.92)
                .build();

        RetrievalCandidate vec2 = RetrievalCandidate.builder()
                .chunkId(vectorOnlyId)
                .content("Vector Only Content")
                .vectorScore(0.88)
                .build();

        List<RetrievalCandidate> result = fusionEngine.fuse(List.of(lex1, lex2), List.of(vec1, vec2));

        assertThat(result).hasSize(3);
        // sharedChunkId was rank 1 in lexical and rank 1 in vector -> must be top rank with highest fused score
        RetrievalCandidate top = result.get(0);
        assertThat(top.getChunkId()).isEqualTo(sharedChunkId);
        assertThat(top.getMatchSource()).isEqualTo(RetrievalMatchSource.HYBRID_BOTH);
        assertThat(top.getFusedScore()).isEqualTo(1.0); // 1.0 because rank 1 in both perfectly equals rrfMax

        // Next candidates are single-source
        assertThat(result.get(1).getMatchSource()).isIn(RetrievalMatchSource.LEXICAL_ONLY, RetrievalMatchSource.SEMANTIC_ONLY);
        assertThat(result.get(1).getFusedScore()).isLessThan(top.getFusedScore());
    }

    @Test
    @DisplayName("fuse - handles empty lists gracefully")
    void testEmptyCandidateLists() {
        UUID chunkId = UUID.randomUUID();
        RetrievalCandidate lex = RetrievalCandidate.builder()
                .chunkId(chunkId)
                .content("Only Lexical")
                .lexicalScore(0.6)
                .build();

        List<RetrievalCandidate> resultWithEmptyVec = fusionEngine.fuse(List.of(lex), List.of());
        assertThat(resultWithEmptyVec).hasSize(1);
        assertThat(resultWithEmptyVec.get(0).getMatchSource()).isEqualTo(RetrievalMatchSource.LEXICAL_ONLY);

        List<RetrievalCandidate> resultBothEmpty = fusionEngine.fuse(List.of(), List.of());
        assertThat(resultBothEmpty).isEmpty();
    }

    @Test
    @DisplayName("fuse - scores are strictly normalized within [0.0, 1.0]")
    void testScoreNormalization() {
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();

        List<RetrievalCandidate> lex = List.of(
                RetrievalCandidate.builder().chunkId(c1).content("A").build()
        );
        List<RetrievalCandidate> vec = List.of(
                RetrievalCandidate.builder().chunkId(c2).content("B").build()
        );

        List<RetrievalCandidate> result = fusionEngine.fuse(lex, vec);

        for (RetrievalCandidate c : result) {
            assertThat(c.getFusedScore()).isBetween(0.0, 1.0);
        }
    }
}
