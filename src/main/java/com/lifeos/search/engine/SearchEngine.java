package com.lifeos.search.engine;

import com.lifeos.search.dto.SearchCountSummaryDto;
import com.lifeos.search.dto.SearchQuery;
import com.lifeos.search.dto.SearchResultItemDto;
import com.lifeos.search.dto.SearchSuggestionDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

/**
 * Core search engine SPI / port.
 * <p>
 * Decouples query orchestration from physical query execution, allowing
 * PostgreSQL full-text search (Phase 10) to seamlessly evolve into
 * Hybrid Vector RAG (Phase 12) without altering domain service contracts.
 */
public interface SearchEngine {

    /**
     * Executes a unified cross-domain query for the specified user.
     */
    Page<SearchResultItemDto> search(UUID userId, SearchQuery query);

    /**
     * Aggregates matching result counts partitioned by entity type.
     */
    SearchCountSummaryDto countByEntity(UUID userId, SearchQuery query);

    /**
     * Retrieves autocomplete typeahead suggestions.
     */
    List<SearchSuggestionDto> suggest(UUID userId, String prefix, int limit);
}
