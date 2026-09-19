package com.lifeos.search.service;

import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.dependent.entity.DependentEntity;
import com.lifeos.dependent.repository.DependentRepository;
import com.lifeos.search.dto.SearchCountSummaryDto;
import com.lifeos.search.dto.SearchQuery;
import com.lifeos.search.dto.SearchResultItemDto;
import com.lifeos.search.dto.SearchSuggestionDto;
import com.lifeos.search.engine.SearchEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrator service for cross-domain unified search.
 * <p>
 * Enforces multi-tenant authorization, validates filter boundaries,
 * invokes the SearchEngine, and enriches search results with related metadata.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedSearchService {

    private final SearchEngine searchEngine;
    private final DependentRepository dependentRepository;

    @Transactional(readOnly = true)
    public Page<SearchResultItemDto> search(UUID userId, SearchQuery query) {
        validateQuery(userId, query);

        Page<SearchResultItemDto> page = searchEngine.search(userId, query);

        // Enrich results with dependent names if applicable
        enrichDependentNames(userId, page.getContent());

        return page;
    }

    @Transactional(readOnly = true)
    public SearchCountSummaryDto countByEntity(UUID userId, SearchQuery query) {
        validateQuery(userId, query);
        return searchEngine.countByEntity(userId, query);
    }

    @Transactional(readOnly = true)
    public List<SearchSuggestionDto> suggest(UUID userId, String prefix, int limit) {
        return searchEngine.suggest(userId, prefix, limit);
    }

    private void validateQuery(UUID userId, SearchQuery query) {
        if (query.getStartDate() != null && query.getEndDate() != null 
                && query.getStartDate().isAfter(query.getEndDate())) {
            throw new IllegalArgumentException("startDate must not be after endDate");
        }

        if (query.getMinAmount() != null && query.getMinAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("minAmount must not be negative");
        }

        if (query.getMaxAmount() != null && query.getMaxAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("maxAmount must not be negative");
        }

        if (query.getMinAmount() != null && query.getMaxAmount() != null 
                && query.getMinAmount().compareTo(query.getMaxAmount()) > 0) {
            throw new IllegalArgumentException("minAmount must not be greater than maxAmount");
        }

        if (query.getDependentId() != null) {
            boolean exists = dependentRepository.findByIdAndUserIdAndIsDeletedFalse(query.getDependentId(), userId).isPresent();
            if (!exists) {
                throw new ResourceNotFoundException("Dependent not found with ID: " + query.getDependentId());
            }
        }
    }

    private void enrichDependentNames(UUID userId, List<SearchResultItemDto> items) {
        List<UUID> dependentIds = items.stream()
                .map(SearchResultItemDto::getDependentId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        if (dependentIds.isEmpty()) {
            return;
        }

        Map<UUID, String> dependentMap = dependentRepository.findAllByUserIdAndIsDeletedFalse(userId).stream()
                .collect(Collectors.toMap(DependentEntity::getId, DependentEntity::getFullName));

        for (SearchResultItemDto item : items) {
            if (item.getDependentId() != null) {
                item.setDependentName(dependentMap.get(item.getDependentId()));
            }
        }
    }
}
