package com.lifeos.rag.retrieval.service;

import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.dependent.repository.DependentRepository;
import com.lifeos.document.embedding.EmbeddingProvider;
import com.lifeos.rag.retrieval.citation.CitationGenerator;
import com.lifeos.rag.retrieval.config.RetrievalProperties;
import com.lifeos.rag.retrieval.dto.RetrievalCitationDto;
import com.lifeos.rag.retrieval.dto.RetrievalFilters;
import com.lifeos.rag.retrieval.dto.RetrievalMetadataDto;
import com.lifeos.rag.retrieval.dto.RetrievalMode;
import com.lifeos.rag.retrieval.dto.RetrievalRequest;
import com.lifeos.rag.retrieval.dto.RetrievalResponse;
import com.lifeos.rag.retrieval.dto.RetrievedChunkDto;
import com.lifeos.rag.retrieval.fusion.CandidateFusionEngine;
import com.lifeos.rag.retrieval.model.RetrievalCandidate;
import com.lifeos.rag.retrieval.processor.QueryProcessor;
import com.lifeos.rag.retrieval.processor.QueryProcessor.ProcessedQuery;
import com.lifeos.rag.retrieval.rerank.Reranker;
import com.lifeos.rag.retrieval.repository.JdbcRetrievalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Production-grade Hybrid RAG Retrieval Engine orchestration service.
 * Coordinates query processing, pushdown authorization, dual lexical/vector search,
 * RRF fusion, local reranking, thresholding, and provenance citation generation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRetrievalService implements RetrievalService {

    private final JdbcRetrievalRepository retrievalRepository;
    private final QueryProcessor queryProcessor;
    private final CandidateFusionEngine fusionEngine;
    private final Reranker reranker;
    private final CitationGenerator citationGenerator;
    private final EmbeddingProvider embeddingProvider;
    private final RetrievalProperties properties;
    private final DependentRepository dependentRepository;

    @Override
    @Transactional(readOnly = true)
    public RetrievalResponse retrieve(UUID userId, RetrievalRequest request) {
        long startTotalNano = System.nanoTime();

        // 1. Validate request boundaries and authorization prerequisites
        validateRequest(userId, request);

        // 2. Deterministic Query Processing
        ProcessedQuery processedQuery = queryProcessor.process(request.getQuery());

        RetrievalMode mode = request.getMode() != null ? request.getMode() : RetrievalMode.HYBRID;
        RetrievalFilters filters = request.getFilters();
        int requestedTopK = request.getTopK() > 0 ? request.getTopK() : 5;

        List<RetrievalCandidate> lexicalCandidates = Collections.emptyList();
        List<RetrievalCandidate> vectorCandidates = Collections.emptyList();

        long lexicalLatencyMs = 0;
        long vectorLatencyMs = 0;
        boolean degraded = false;
        String degradationReason = null;

        // 3. Lexical Retrieval Pass (PostgreSQL FTS)
        if (mode == RetrievalMode.HYBRID || mode == RetrievalMode.LEXICAL) {
            long startLexNano = System.nanoTime();
            lexicalCandidates = retrievalRepository.findLexicalCandidates(
                    userId, processedQuery.normalizedQuery(), filters, properties.getLexicalTopK()
            );
            lexicalLatencyMs = (System.nanoTime() - startLexNano) / 1_000_000;
        }

        // 4. Semantic Retrieval Pass (pgvector HNSW)
        if (mode == RetrievalMode.HYBRID || mode == RetrievalMode.SEMANTIC) {
            long startVecNano = System.nanoTime();
            try {
                float[] queryEmbedding = embeddingProvider.generateEmbedding(processedQuery.normalizedQuery());
                double vecThreshold = request.getMinRelevanceScore() != null
                        ? request.getMinRelevanceScore()
                        : properties.getMinSimilarityThreshold();
                vectorCandidates = retrievalRepository.findVectorCandidates(
                        userId, queryEmbedding, filters, properties.getVectorTopK(), vecThreshold
                );
            } catch (Exception e) {
                log.warn("Vector embedding generation failed for user [{}]: {}", userId, e.getMessage());
                if (mode == RetrievalMode.HYBRID) {
                    degraded = true;
                    degradationReason = "Embedding provider unavailable; degraded to lexical FTS: " + e.getMessage();
                } else {
                    degraded = true;
                    degradationReason = "Embedding provider failed: " + e.getMessage();
                }
            }
            vectorLatencyMs = (System.nanoTime() - startVecNano) / 1_000_000;
        }

        // 5. Candidate Fusion (Reciprocal Rank Fusion)
        long startFusionNano = System.nanoTime();
        List<RetrievalCandidate> fusedCandidates;
        if (mode == RetrievalMode.HYBRID) {
            fusedCandidates = fusionEngine.fuse(lexicalCandidates, vectorCandidates);
        } else if (mode == RetrievalMode.LEXICAL) {
            fusedCandidates = new ArrayList<>(lexicalCandidates);
            for (RetrievalCandidate c : fusedCandidates) {
                c.setFusedScore(c.getLexicalScore());
            }
        } else {
            fusedCandidates = new ArrayList<>(vectorCandidates);
            for (RetrievalCandidate c : fusedCandidates) {
                c.setFusedScore(c.getVectorScore());
            }
        }
        long fusionLatencyMs = (System.nanoTime() - startFusionNano) / 1_000_000;

        // 6. Reranking Stage
        long startRerankNano = System.nanoTime();
        int rerankLimit = Math.min(properties.getRerankLimit(), fusedCandidates.size());
        List<RetrievalCandidate> candidatesToRerank = fusedCandidates.subList(0, rerankLimit);
        List<RetrievalCandidate> rerankedCandidates = reranker.rerank(processedQuery, candidatesToRerank, requestedTopK);
        long rerankLatencyMs = (System.nanoTime() - startRerankNano) / 1_000_000;

        // 7. Defensive Multi-Tenant Authorization Verification
        List<RetrievalCandidate> authorizedCandidates = new ArrayList<>(rerankedCandidates.size());
        for (RetrievalCandidate c : rerankedCandidates) {
            if (userId.equals(c.getUserId())) {
                authorizedCandidates.add(c);
            } else {
                log.error("CRITICAL SECURITY ALARM: Candidate chunk [{}] for user [{}] bypassed SQL tenant filter for requester [{}]",
                        c.getChunkId(), c.getUserId(), userId);
            }
        }

        // 8. Relevance Threshold Cutoff & NO_RELEVANT_CONTEXT Check
        double effectiveMinScore = request.getMinRelevanceScore() != null
                ? request.getMinRelevanceScore()
                : properties.getMinRerankScoreThreshold();

        List<RetrievalCandidate> qualifiedCandidates = authorizedCandidates.stream()
                .filter(c -> {
                    double score = c.getRerankScore() != null ? c.getRerankScore()
                            : (c.getFusedScore() != null ? c.getFusedScore() : 0.0);
                    return score >= effectiveMinScore;
                })
                .toList();

        // 9. Context Length Guard
        List<RetrievalCandidate> finalCandidates = new ArrayList<>();
        int currentTotalChars = 0;
        for (RetrievalCandidate c : qualifiedCandidates) {
            int chunkChars = c.getContent() != null ? c.getContent().length() : 0;
            if (currentTotalChars + chunkChars <= properties.getMaxContextChars() || finalCandidates.isEmpty()) {
                finalCandidates.add(c);
                currentTotalChars += chunkChars;
            } else {
                log.debug("Max context length reached ({} chars). Skipping remaining candidates.", currentTotalChars);
                break;
            }
        }

        // 10. Citation & Output Mapping
        boolean hasContext = !finalCandidates.isEmpty();
        String noContextReason = null;
        if (!hasContext) {
            if (lexicalCandidates.isEmpty() && vectorCandidates.isEmpty()) {
                noContextReason = "NO_MATCHING_CONTEXT_FOUND";
            } else {
                noContextReason = "ALL_CANDIDATES_BELOW_RELEVANCE_THRESHOLD";
            }
        }

        List<RetrievalCitationDto> citations = citationGenerator.generateCitations(finalCandidates);
        List<RetrievedChunkDto> resultDtos = finalCandidates.stream()
                .map(c -> mapToDto(c, request.isIncludeContent()))
                .toList();

        long totalTimeMs = (System.nanoTime() - startTotalNano) / 1_000_000;

        RetrievalMetadataDto metadata = RetrievalMetadataDto.builder()
                .totalCandidatesFound(lexicalCandidates.size() + vectorCandidates.size())
                .lexicalCandidatesCount(lexicalCandidates.size())
                .vectorCandidatesCount(vectorCandidates.size())
                .executionTimeMs(totalTimeMs)
                .lexicalLatencyMs(lexicalLatencyMs)
                .vectorLatencyMs(vectorLatencyMs)
                .fusionLatencyMs(fusionLatencyMs)
                .rerankLatencyMs(rerankLatencyMs)
                .degraded(degraded)
                .degradationReason(degradationReason)
                .build();

        log.info("[RAG-RETRIEVE] User [{}] executed [{}] retrieval for query [{}] -> {} lexical, {} vector candidates -> {} fused -> {} final chunks in {}ms",
                userId, mode, processedQuery.normalizedQuery(), lexicalCandidates.size(), vectorCandidates.size(),
                fusedCandidates.size(), finalCandidates.size(), totalTimeMs);

        return RetrievalResponse.builder()
                .query(processedQuery.normalizedQuery())
                .retrievalMode(mode)
                .hasRelevantContext(hasContext)
                .noContextReason(noContextReason)
                .results(resultDtos)
                .citations(citations)
                .metadata(metadata)
                .build();
    }

    private void validateRequest(UUID userId, RetrievalRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Retrieval request must not be null");
        }

        RetrievalFilters filters = request.getFilters();
        if (filters != null) {
            if (filters.getStartDate() != null && filters.getEndDate() != null
                    && filters.getStartDate().isAfter(filters.getEndDate())) {
                throw new IllegalArgumentException("startDate must not be after endDate");
            }

            if (filters.getDependentId() != null) {
                boolean exists = dependentRepository.findByIdAndUserIdAndIsDeletedFalse(filters.getDependentId(), userId).isPresent();
                if (!exists) {
                    throw new ResourceNotFoundException("Dependent not found with ID: " + filters.getDependentId());
                }
            }
        }
    }

    private RetrievedChunkDto mapToDto(RetrievalCandidate c, boolean includeContent) {
        double score = c.getRerankScore() != null ? c.getRerankScore()
                : (c.getFusedScore() != null ? c.getFusedScore() : 0.0);

        return RetrievedChunkDto.builder()
                .chunkId(c.getChunkId())
                .documentId(c.getDocumentId())
                .documentVersion(c.getDocumentVersion())
                .chunkIndex(c.getChunkIndex())
                .documentTitle(c.getDocumentTitle())
                .originalFilename(c.getOriginalFilename())
                .category(c.getDocumentCategory())
                .documentType(c.getDocumentType())
                .dependentId(c.getDependentId())
                .pageNumber(c.getPageNumber())
                .sectionTitle(c.getSectionTitle())
                .tokenCount(c.getTokenCount())
                .content(includeContent ? c.getContent() : null)
                .snippet(c.getSnippet())
                .relevanceScore(score)
                .matchSource(c.getMatchSource())
                .metadata(c.getMetadata())
                .build();
    }
}
