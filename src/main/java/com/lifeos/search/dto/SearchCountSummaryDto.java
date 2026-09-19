package com.lifeos.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Faceted search count summary partitioned across domain entity types.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCountSummaryDto {

    private long totalCount;

    private Map<SearchEntityType, Long> countsByEntity;
}
