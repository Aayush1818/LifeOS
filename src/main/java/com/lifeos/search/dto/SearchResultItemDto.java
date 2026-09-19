package com.lifeos.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Unified enveloped search result contract returned for any matching LifeOS entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultItemDto {

    private SearchEntityType entityType;

    private UUID entityId;

    private String title;

    private String subtitle;

    private String snippet;

    private String category;

    private String status;

    private BigDecimal amount;

    private String currency;

    private LocalDate eventDate;

    private UUID dependentId;

    private String dependentName;

    private String entityRoute;

    private Double relevanceScore;

    private OffsetDateTime createdAt;

    private Map<String, Object> metadata;
}
