package com.lifeos.ai.grounding;

import com.lifeos.rag.retrieval.dto.RetrievalCitationDto;
import com.lifeos.rag.retrieval.dto.RetrievedChunkDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Structured container holding the formatted context text for prompt assembly
 * and the 1-indexed lookup maps for subsequent citation validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssembledContext {

    /**
     * Formatted string block with XML untrusted reference sources.
     */
    private String formattedContext;

    /**
     * Map from 1-based source index (e.g. 1, 2) to the retrieved chunk.
     */
    @Builder.Default
    private Map<Integer, RetrievedChunkDto> sourceMap = new HashMap<>();

    /**
     * Map from 1-based source index to the Phase 12 retrieval citation.
     */
    @Builder.Default
    private Map<Integer, RetrievalCitationDto> citationMap = new HashMap<>();

    @Builder.Default
    private int sourceCount = 0;

    @Builder.Default
    private int totalChars = 0;
}
