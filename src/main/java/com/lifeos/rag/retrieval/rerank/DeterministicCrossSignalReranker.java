package com.lifeos.rag.retrieval.rerank;

import com.lifeos.rag.retrieval.model.RetrievalCandidate;
import com.lifeos.rag.retrieval.processor.QueryProcessor.ProcessedQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * High-performance deterministic local reranker.
 * Evaluates exact phrase matches, section/title term alignment, identifier matches,
 * and version signals without requiring external API calls or GPU models.
 */
@Slf4j
@Component
public class DeterministicCrossSignalReranker implements Reranker {

    private static final double EXACT_PHRASE_BOOST = 1.25;
    private static final double IDENTIFIER_BOOST = 1.20;
    private static final double TITLE_SECTION_BOOST = 1.15;
    private static final double VERSION_RECENCY_BOOST = 1.05;

    @Override
    public List<RetrievalCandidate> rerank(ProcessedQuery query, List<RetrievalCandidate> candidates, int topN) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<RetrievalCandidate> reranked = new ArrayList<>(candidates.size());
        String normalizedQueryLower = query.normalizedQuery().toLowerCase();

        for (RetrievalCandidate c : candidates) {
            double baseScore = c.getFusedScore() != null ? c.getFusedScore() : 0.5;
            double boost = 1.0;

            String contentLower = c.getContent() != null ? c.getContent().toLowerCase() : "";
            String titleLower = c.getDocumentTitle() != null ? c.getDocumentTitle().toLowerCase() : "";
            String sectionLower = c.getSectionTitle() != null ? c.getSectionTitle().toLowerCase() : "";

            // 1. Exact phrase match boost
            boolean exactPhraseMatched = false;
            if (!query.exactPhrases().isEmpty()) {
                for (String phrase : query.exactPhrases()) {
                    if (contentLower.contains(phrase)) {
                        boost *= EXACT_PHRASE_BOOST;
                        exactPhraseMatched = true;
                        break;
                    }
                }
            } else if (normalizedQueryLower.length() > 6 && contentLower.contains(normalizedQueryLower)) {
                boost *= EXACT_PHRASE_BOOST;
                exactPhraseMatched = true;
            }

            // 2. Identifier match boost
            if (!query.identifiers().isEmpty()) {
                for (String id : query.identifiers()) {
                    if (contentLower.contains(id)) {
                        boost *= IDENTIFIER_BOOST;
                        break;
                    }
                }
            }

            // 3. Document title or section title term match
            if (!titleLower.isEmpty() && titleLower.contains(normalizedQueryLower)
                    || (!sectionLower.isEmpty() && sectionLower.contains(normalizedQueryLower))) {
                boost *= TITLE_SECTION_BOOST;
            }

            // 4. Version recency boost (version >= 1 gets slight recency weight)
            if (c.getDocumentVersion() != null && c.getDocumentVersion() >= 1) {
                boost *= VERSION_RECENCY_BOOST;
            }

            double finalScore = Math.min(1.0, baseScore * boost);
            c.setRerankScore(Math.round(finalScore * 1000.0) / 1000.0);
            reranked.add(c);
        }

        reranked.sort(Comparator
                .comparing(RetrievalCandidate::getRerankScore, Comparator.reverseOrder())
                .thenComparing(RetrievalCandidate::getChunkIndex, Comparator.nullsLast(Comparator.naturalOrder())));

        int limit = Math.min(topN, reranked.size());
        return reranked.subList(0, limit);
    }
}
