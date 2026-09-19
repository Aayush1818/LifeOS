package com.lifeos.rag.retrieval.fusion;

import com.lifeos.rag.retrieval.config.RetrievalProperties;
import com.lifeos.rag.retrieval.dto.RetrievalMatchSource;
import com.lifeos.rag.retrieval.model.RetrievalCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Scale-invariant Reciprocal Rank Fusion (RRF) engine.
 * Combines ranked candidate lists from lexical and vector retrieval passes,
 * deduplicates by chunkId, and computes deterministic normalized relevance scores.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateFusionEngine {

    private final RetrievalProperties properties;

    /**
     * Merges and fuses lexical and vector candidates using Reciprocal Rank Fusion.
     *
     * @param lexicalCandidates ranked candidates from PostgreSQL FTS
     * @param vectorCandidates  ranked candidates from pgvector
     * @return deduplicated candidates ranked by normalized fused RRF score
     */
    public List<RetrievalCandidate> fuse(
            List<RetrievalCandidate> lexicalCandidates,
            List<RetrievalCandidate> vectorCandidates) {

        int k = Math.max(1, properties.getRrfConstantK());
        double wLex = Math.max(0.0, properties.getLexicalWeight());
        double wVec = Math.max(0.0, properties.getVectorWeight());

        // Maximum achievable RRF score (rank 1 in both lists)
        double rrfMax = (wLex / (k + 1.0)) + (wVec / (k + 1.0));
        if (rrfMax <= 0.0) {
            rrfMax = 1.0;
        }

        Map<UUID, RetrievalCandidate> merged = new LinkedHashMap<>();

        // 1. Process Lexical Candidates
        if (lexicalCandidates != null) {
            for (int i = 0; i < lexicalCandidates.size(); i++) {
                RetrievalCandidate c = lexicalCandidates.get(i);
                int rank = i + 1;
                c.setLexicalRank(rank);
                c.setMatchSource(RetrievalMatchSource.LEXICAL_ONLY);

                double rrfScore = wLex / (k + rank);
                c.setFusedScore(rrfScore);

                merged.put(c.getChunkId(), c);
            }
        }

        // 2. Process Vector Candidates & Merge
        if (vectorCandidates != null) {
            for (int i = 0; i < vectorCandidates.size(); i++) {
                RetrievalCandidate vecCandidate = vectorCandidates.get(i);
                int rank = i + 1;
                vecCandidate.setVectorRank(rank);

                double vecRrfContribution = wVec / (k + rank);

                RetrievalCandidate existing = merged.get(vecCandidate.getChunkId());
                if (existing != null) {
                    // Duplicate found in both lexical and semantic sets
                    existing.setVectorScore(vecCandidate.getVectorScore());
                    existing.setVectorRank(rank);
                    existing.setMatchSource(RetrievalMatchSource.HYBRID_BOTH);

                    double combinedRrf = existing.getFusedScore() + vecRrfContribution;
                    existing.setFusedScore(combinedRrf);

                    // Prefer lexical snippet with <b> highlights if available
                    if (existing.getSnippet() == null || existing.getSnippet().isBlank()) {
                        existing.setSnippet(vecCandidate.getSnippet());
                    }
                } else {
                    vecCandidate.setMatchSource(RetrievalMatchSource.SEMANTIC_ONLY);
                    vecCandidate.setFusedScore(vecRrfContribution);
                    merged.put(vecCandidate.getChunkId(), vecCandidate);
                }
            }
        }

        // 3. Normalize scores to [0.0, 1.0] and sort descending
        final double finalRrfMax = rrfMax;
        List<RetrievalCandidate> fusedList = new ArrayList<>(merged.values());
        for (RetrievalCandidate c : fusedList) {
            double normalized = c.getFusedScore() / finalRrfMax;
            c.setFusedScore(Math.round(Math.min(1.0, normalized) * 1000.0) / 1000.0);
        }

        fusedList.sort(Comparator
                .comparing(RetrievalCandidate::getFusedScore, Comparator.reverseOrder())
                .thenComparing(RetrievalCandidate::getChunkIndex, Comparator.nullsLast(Comparator.naturalOrder())));

        log.debug("RRF fused {} lexical and {} vector candidates into {} unique candidates",
                lexicalCandidates != null ? lexicalCandidates.size() : 0,
                vectorCandidates != null ? vectorCandidates.size() : 0,
                fusedList.size());

        return fusedList;
    }
}
