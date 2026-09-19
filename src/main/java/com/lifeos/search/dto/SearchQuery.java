package com.lifeos.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Encapsulates parameters for unified cross-domain search.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchQuery {

    private String queryText;

    private Set<SearchEntityType> entities;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private String currency;

    private String category;

    private String status;

    private UUID dependentId;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    @Builder.Default
    private SearchSortOption sortBy = SearchSortOption.RELEVANCE;
}
