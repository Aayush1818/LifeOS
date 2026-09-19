package com.lifeos.rag.retrieval.citation;

import com.lifeos.rag.retrieval.dto.RetrievalCitationDto;
import com.lifeos.rag.retrieval.model.RetrievalCandidate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates verified, structured provenance citations from retrieved candidates
 * for grounding downstream LLM answers in Phase 13.
 */
@Component
public class CitationGenerator {

    /**
     * Converts ranked candidates into traceable citation DTOs.
     */
    public List<RetrievalCitationDto> generateCitations(List<RetrievalCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<RetrievalCitationDto> citations = new ArrayList<>(candidates.size());

        for (RetrievalCandidate c : candidates) {
            String title = c.getDocumentTitle() != null ? c.getDocumentTitle() : "Document";
            int page = c.getPageNumber() != null ? c.getPageNumber() : 1;
            int version = c.getDocumentVersion() != null ? c.getDocumentVersion() : 1;
            int index = c.getChunkIndex() != null ? c.getChunkIndex() : 0;
            String section = c.getSectionTitle();

            StringBuilder sb = new StringBuilder("[");
            sb.append(title).append(", Page ").append(page);
            if (section != null && !section.trim().isEmpty()) {
                sb.append(", Section: ").append(section.trim());
            }
            sb.append(" (v").append(version).append(", Chunk #").append(index).append(")]");

            double score = c.getRerankScore() != null ? c.getRerankScore()
                    : (c.getFusedScore() != null ? c.getFusedScore() : 0.0);

            citations.add(RetrievalCitationDto.builder()
                    .documentId(c.getDocumentId())
                    .documentVersion(c.getDocumentVersion())
                    .chunkId(c.getChunkId())
                    .chunkIndex(c.getChunkIndex())
                    .documentTitle(c.getDocumentTitle())
                    .pageNumber(page)
                    .sectionTitle(section)
                    .sourceCitation(sb.toString())
                    .relevanceScore(score)
                    .matchSource(c.getMatchSource())
                    .build());
        }

        return citations;
    }
}
