package com.lifeos.rag.retrieval;

import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.dependent.entity.DependentEntity;
import com.lifeos.dependent.repository.DependentRepository;
import com.lifeos.document.embedding.EmbeddingProvider;
import com.lifeos.rag.retrieval.citation.CitationGenerator;
import com.lifeos.rag.retrieval.config.RetrievalProperties;
import com.lifeos.rag.retrieval.dto.RetrievalFilters;
import com.lifeos.rag.retrieval.dto.RetrievalMode;
import com.lifeos.rag.retrieval.dto.RetrievalRequest;
import com.lifeos.rag.retrieval.dto.RetrievalResponse;
import com.lifeos.rag.retrieval.fusion.CandidateFusionEngine;
import com.lifeos.rag.retrieval.model.RetrievalCandidate;
import com.lifeos.rag.retrieval.processor.QueryProcessor;
import com.lifeos.rag.retrieval.repository.JdbcRetrievalRepository;
import com.lifeos.rag.retrieval.rerank.DeterministicCrossSignalReranker;
import com.lifeos.rag.retrieval.rerank.Reranker;
import com.lifeos.rag.retrieval.service.HybridRetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridRetrievalServiceTest {

    @Mock
    private JdbcRetrievalRepository retrievalRepository;

    @Mock
    private EmbeddingProvider embeddingProvider;

    @Mock
    private DependentRepository dependentRepository;

    private QueryProcessor queryProcessor;
    private CandidateFusionEngine fusionEngine;
    private Reranker reranker;
    private CitationGenerator citationGenerator;
    private RetrievalProperties properties;
    private HybridRetrievalService retrievalService;

    private final UUID testUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        queryProcessor = new QueryProcessor();
        properties = new RetrievalProperties();
        properties.setMinRerankScoreThreshold(0.20);
        fusionEngine = new CandidateFusionEngine(properties);
        reranker = new DeterministicCrossSignalReranker();
        citationGenerator = new CitationGenerator();

        retrievalService = new HybridRetrievalService(
                retrievalRepository,
                queryProcessor,
                fusionEngine,
                reranker,
                citationGenerator,
                embeddingProvider,
                properties,
                dependentRepository
        );
    }

    @Test
    @DisplayName("retrieve - HYBRID mode executes both lexical and vector search and returns fused results")
    void testHybridRetrievalSuccess() {
        UUID chunkId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        RetrievalCandidate candidate = RetrievalCandidate.builder()
                .chunkId(chunkId)
                .documentId(docId)
                .documentVersion(1)
                .chunkIndex(1)
                .userId(testUserId)
                .documentTitle("Health Policy")
                .content("Inpatient surgery has a $500 deductible.")
                .snippet("Inpatient surgery has a <b>$500 deductible</b>.")
                .pageNumber(1)
                .sectionTitle("Coverage")
                .lexicalScore(0.85)
                .build();

        float[] mockVector = new float[]{0.1f, 0.2f};
        when(embeddingProvider.generateEmbedding(anyString())).thenReturn(mockVector);
        when(retrievalRepository.findLexicalCandidates(eq(testUserId), anyString(), any(), anyInt()))
                .thenReturn(List.of(candidate));
        when(retrievalRepository.findVectorCandidates(eq(testUserId), any(), any(), anyInt(), anyDouble()))
                .thenReturn(List.of(candidate));

        RetrievalRequest request = RetrievalRequest.builder()
                .query("surgery deductible")
                .mode(RetrievalMode.HYBRID)
                .topK(5)
                .build();

        RetrievalResponse response = retrievalService.retrieve(testUserId, request);

        assertThat(response.isHasRelevantContext()).isTrue();
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getChunkId()).isEqualTo(chunkId);
        assertThat(response.getCitations()).hasSize(1);
        assertThat(response.getCitations().get(0).getSourceCitation()).contains("Health Policy");
        assertThat(response.getMetadata().isDegraded()).isFalse();
    }

    @Test
    @DisplayName("retrieve - degrades gracefully to LEXICAL when embedding provider fails")
    void testEmbeddingFailureGracefulDegradation() {
        UUID chunkId = UUID.randomUUID();
        RetrievalCandidate candidate = RetrievalCandidate.builder()
                .chunkId(chunkId)
                .documentId(UUID.randomUUID())
                .documentVersion(1)
                .chunkIndex(1)
                .userId(testUserId)
                .documentTitle("Auto Insurance")
                .content("Collision deductible is $1000.")
                .lexicalScore(0.75)
                .build();

        when(retrievalRepository.findLexicalCandidates(eq(testUserId), anyString(), any(), anyInt()))
                .thenReturn(List.of(candidate));
        when(embeddingProvider.generateEmbedding(anyString()))
                .thenThrow(new RuntimeException("OpenAI API Connection Timeout"));

        RetrievalRequest request = RetrievalRequest.builder()
                .query("collision deductible")
                .mode(RetrievalMode.HYBRID)
                .build();

        RetrievalResponse response = retrievalService.retrieve(testUserId, request);

        assertThat(response.isHasRelevantContext()).isTrue();
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getMetadata().isDegraded()).isTrue();
        assertThat(response.getMetadata().getDegradationReason()).contains("Embedding provider unavailable");
    }

    @Test
    @DisplayName("retrieve - sets hasRelevantContext=false when all candidates fall below threshold")
    void testAllCandidatesBelowThreshold() {
        UUID chunkId = UUID.randomUUID();
        RetrievalCandidate candidate = RetrievalCandidate.builder()
                .chunkId(chunkId)
                .userId(testUserId)
                .content("Unrelated text about gardening")
                .lexicalScore(0.01)
                .build();

        when(retrievalRepository.findLexicalCandidates(eq(testUserId), anyString(), any(), anyInt()))
                .thenReturn(List.of(candidate));
        when(embeddingProvider.generateEmbedding(anyString())).thenReturn(new float[]{0.1f});
        when(retrievalRepository.findVectorCandidates(eq(testUserId), any(), any(), anyInt(), anyDouble()))
                .thenReturn(List.of());

        RetrievalRequest request = RetrievalRequest.builder()
                .query("heart surgery")
                .minRelevanceScore(0.80) // High threshold
                .build();

        RetrievalResponse response = retrievalService.retrieve(testUserId, request);

        assertThat(response.isHasRelevantContext()).isFalse();
        assertThat(response.getNoContextReason()).isEqualTo("ALL_CANDIDATES_BELOW_RELEVANCE_THRESHOLD");
        assertThat(response.getResults()).isEmpty();
        assertThat(response.getCitations()).isEmpty();
    }

    @Test
    @DisplayName("retrieve - throws 404 when dependentId does not belong to user")
    void testForeignDependentThrows404() {
        UUID foreignDependentId = UUID.randomUUID();
        when(dependentRepository.findByIdAndUserIdAndIsDeletedFalse(foreignDependentId, testUserId))
                .thenReturn(Optional.empty());

        RetrievalRequest request = RetrievalRequest.builder()
                .query("medical records")
                .filters(RetrievalFilters.builder().dependentId(foreignDependentId).build())
                .build();

        assertThatThrownBy(() -> retrievalService.retrieve(testUserId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(foreignDependentId.toString());
    }
}
