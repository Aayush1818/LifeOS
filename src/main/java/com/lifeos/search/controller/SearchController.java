package com.lifeos.search.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.search.dto.SearchCountSummaryDto;
import com.lifeos.search.dto.SearchEntityType;
import com.lifeos.search.dto.SearchQuery;
import com.lifeos.search.dto.SearchResultItemDto;
import com.lifeos.search.dto.SearchSortOption;
import com.lifeos.search.dto.SearchSuggestionDto;
import com.lifeos.search.service.UnifiedSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * REST API for Unified Cross-Domain Search across all LifeOS entities.
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Unified Search", description = "Endpoints for searching across all personal domains and entities")
public class SearchController {

    private final UnifiedSearchService unifiedSearchService;

    @GetMapping
    @Operation(summary = "Search across all personal entities with lexical full-text and structured filters")
    public ResponseEntity<ApiResponse<Page<SearchResultItemDto>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Set<SearchEntityType> entities,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID dependentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "RELEVANCE") SearchSortOption sortBy) {

        UUID userId = SecurityUtils.getCurrentUserId();

        SearchQuery query = SearchQuery.builder()
                .queryText(q)
                .entities(entities)
                .startDate(startDate)
                .endDate(endDate)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .currency(currency)
                .category(category)
                .status(status)
                .dependentId(dependentId)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .build();

        Page<SearchResultItemDto> results = unifiedSearchService.search(userId, query);
        return ResponseEntity.ok(ApiResponse.success(results, "Search completed successfully"));
    }

    @GetMapping("/count")
    @Operation(summary = "Get faceted matching counts partitioned across entity types")
    public ResponseEntity<ApiResponse<SearchCountSummaryDto>> count(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Set<SearchEntityType> entities,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID dependentId) {

        UUID userId = SecurityUtils.getCurrentUserId();

        SearchQuery query = SearchQuery.builder()
                .queryText(q)
                .entities(entities)
                .startDate(startDate)
                .endDate(endDate)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .currency(currency)
                .category(category)
                .status(status)
                .dependentId(dependentId)
                .build();

        SearchCountSummaryDto summary = unifiedSearchService.countByEntity(userId, query);
        return ResponseEntity.ok(ApiResponse.success(summary, "Search count summary retrieved"));
    }

    @GetMapping("/suggest")
    @Operation(summary = "Get autocomplete suggestions for a search term")
    public ResponseEntity<ApiResponse<List<SearchSuggestionDto>>> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit) {

        UUID userId = SecurityUtils.getCurrentUserId();
        List<SearchSuggestionDto> suggestions = unifiedSearchService.suggest(userId, q, limit);
        return ResponseEntity.ok(ApiResponse.success(suggestions, "Suggestions retrieved successfully"));
    }

    @GetMapping("/entities")
    @Operation(summary = "List all supported search entity types")
    public ResponseEntity<ApiResponse<List<String>>> listSupportedEntities() {
        List<String> entities = Arrays.stream(SearchEntityType.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(entities, "Supported search entity types"));
    }
}
