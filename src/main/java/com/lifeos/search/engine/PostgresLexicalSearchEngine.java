package com.lifeos.search.engine;

import com.lifeos.search.dto.SearchCountSummaryDto;
import com.lifeos.search.dto.SearchEntityType;
import com.lifeos.search.dto.SearchQuery;
import com.lifeos.search.dto.SearchResultItemDto;
import com.lifeos.search.dto.SearchSortOption;
import com.lifeos.search.dto.SearchSuggestionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Production-grade PostgreSQL full-text and structured lexical search engine.
 * <p>
 * Leverages PostgreSQL 16/18 tsvector, websearch_to_tsquery, ts_rank_cd,
 * ts_headline, and GIN indexes over lifeos_unified_search_view.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostgresLexicalSearchEngine implements SearchEngine {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Page<SearchResultItemDto> search(UUID userId, SearchQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);

        StringBuilder whereClause = new StringBuilder("WHERE user_id = :userId AND is_deleted = false");
        buildFilterPredicates(query, params, whereClause);

        boolean hasQueryText = query.getQueryText() != null && !query.getQueryText().trim().isEmpty();

        // 1. Total Count Query
        String countSql = "SELECT COUNT(*) FROM lifeos_unified_search_view " + whereClause;
        Long totalCount = jdbcTemplate.queryForObject(countSql, params, Long.class);
        long total = (totalCount != null) ? totalCount : 0L;

        if (total == 0L) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(query.getPage(), query.getSize()), 0L);
        }

        // 2. Select Projection
        StringBuilder selectSql = new StringBuilder();
        selectSql.append("SELECT entity_type, entity_id, user_id, dependent_id, title, subtitle, content_text, ")
                .append("category_or_type, status, amount, currency, event_date, created_at ");

        if (hasQueryText) {
            selectSql.append(", ts_rank_cd(tsv_content, websearch_to_tsquery('english', :queryText), 32) AS relevance_score, ")
                    .append("ts_headline('english', content_text, websearch_to_tsquery('english', :queryText), ")
                    .append("'StartSel=<b>, StopSel=</b>, MaxWords=35, MinWords=15') AS snippet ");
        } else {
            selectSql.append(", 1.0 AS relevance_score, ")
                    .append("SUBSTRING(content_text FROM 1 FOR 200) AS snippet ");
        }

        selectSql.append("FROM lifeos_unified_search_view ").append(whereClause);

        // 3. Sorting
        selectSql.append(" ").append(buildOrderByClause(query.getSortBy(), hasQueryText));

        // 4. Pagination
        selectSql.append(" LIMIT :limit OFFSET :offset");
        params.addValue("limit", query.getSize());
        params.addValue("offset", (long) query.getPage() * query.getSize());

        List<SearchResultItemDto> results = jdbcTemplate.query(selectSql.toString(), params, (rs, rowNum) -> mapRowToDto(rs));

        return new PageImpl<>(results, PageRequest.of(query.getPage(), query.getSize()), total);
    }

    @Override
    public SearchCountSummaryDto countByEntity(UUID userId, SearchQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);

        StringBuilder whereClause = new StringBuilder("WHERE user_id = :userId AND is_deleted = false");
        buildFilterPredicates(query, params, whereClause);

        String sql = "SELECT entity_type, COUNT(*) AS cnt FROM lifeos_unified_search_view " +
                whereClause + " GROUP BY entity_type";

        Map<SearchEntityType, Long> countsByEntity = new EnumMap<>(SearchEntityType.class);
        long totalCount = 0;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);
        for (Map<String, Object> row : rows) {
            String entityTypeStr = (String) row.get("entity_type");
            Number cnt = (Number) row.get("cnt");
            try {
                SearchEntityType type = SearchEntityType.valueOf(entityTypeStr);
                long count = cnt != null ? cnt.longValue() : 0L;
                countsByEntity.put(type, count);
                totalCount += count;
            } catch (IllegalArgumentException e) {
                log.warn("Unknown entity_type encountered in search count: {}", entityTypeStr);
            }
        }

        return SearchCountSummaryDto.builder()
                .totalCount(totalCount)
                .countsByEntity(countsByEntity)
                .build();
    }

    @Override
    public List<SearchSuggestionDto> suggest(UUID userId, String prefix, int limit) {
        if (prefix == null || prefix.trim().length() < 2) {
            return Collections.emptyList();
        }

        String sql = """
            SELECT DISTINCT title, entity_type, entity_id
            FROM lifeos_unified_search_view
            WHERE user_id = :userId
              AND is_deleted = false
              AND title ILIKE :likePattern
            ORDER BY title ASC
            LIMIT :limit
        """;

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("likePattern", prefix.trim() + "%");
        params.addValue("limit", Math.min(Math.max(limit, 1), 20));

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            String title = rs.getString("title");
            String entityTypeStr = rs.getString("entity_type");
            UUID entityId = rs.getObject("entity_id", UUID.class);

            SearchEntityType entityType = null;
            try {
                entityType = SearchEntityType.valueOf(entityTypeStr);
            } catch (Exception ignored) {}

            return SearchSuggestionDto.builder()
                    .text(title)
                    .entityType(entityType)
                    .entityId(entityId)
                    .build();
        });
    }

    private void buildFilterPredicates(SearchQuery query, MapSqlParameterSource params, StringBuilder whereClause) {
        if (query.getQueryText() != null && !query.getQueryText().trim().isEmpty()) {
            String q = query.getQueryText().trim();
            params.addValue("queryText", q);
            params.addValue("likePattern", "%" + q + "%");

            whereClause.append(" AND (tsv_content @@ websearch_to_tsquery('english', :queryText) ")
                    .append("OR title ILIKE :likePattern ")
                    .append("OR subtitle ILIKE :likePattern)");
        }

        if (query.getEntities() != null && !query.getEntities().isEmpty()) {
            Set<String> entityNames = query.getEntities().stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            params.addValue("entityTypes", entityNames);
            whereClause.append(" AND entity_type IN (:entityTypes)");
        }

        if (query.getStartDate() != null) {
            params.addValue("startDate", query.getStartDate());
            whereClause.append(" AND event_date >= :startDate");
        }

        if (query.getEndDate() != null) {
            params.addValue("endDate", query.getEndDate());
            whereClause.append(" AND event_date <= :endDate");
        }

        if (query.getMinAmount() != null) {
            params.addValue("minAmount", query.getMinAmount());
            whereClause.append(" AND amount >= :minAmount");
        }

        if (query.getMaxAmount() != null) {
            params.addValue("maxAmount", query.getMaxAmount());
            whereClause.append(" AND amount <= :maxAmount");
        }

        if (query.getCurrency() != null && !query.getCurrency().trim().isEmpty()) {
            params.addValue("currency", query.getCurrency().trim().toUpperCase());
            whereClause.append(" AND currency = :currency");
        }

        if (query.getCategory() != null && !query.getCategory().trim().isEmpty()) {
            params.addValue("category", query.getCategory().trim().toUpperCase());
            whereClause.append(" AND category_or_type = :category");
        }

        if (query.getStatus() != null && !query.getStatus().trim().isEmpty()) {
            params.addValue("status", query.getStatus().trim().toUpperCase());
            whereClause.append(" AND status = :status");
        }

        if (query.getDependentId() != null) {
            params.addValue("dependentId", query.getDependentId());
            whereClause.append(" AND dependent_id = :dependentId");
        }
    }

    private String buildOrderByClause(SearchSortOption sortOption, boolean hasQueryText) {
        if (sortOption == null) {
            sortOption = hasQueryText ? SearchSortOption.RELEVANCE : SearchSortOption.DATE_DESC;
        }

        return switch (sortOption) {
            case RELEVANCE -> "ORDER BY relevance_score DESC, created_at DESC";
            case DATE_DESC -> "ORDER BY event_date DESC NULLS LAST, created_at DESC";
            case DATE_ASC -> "ORDER BY event_date ASC NULLS LAST, created_at ASC";
            case AMOUNT_DESC -> "ORDER BY amount DESC NULLS LAST, created_at DESC";
            case AMOUNT_ASC -> "ORDER BY amount ASC NULLS LAST, created_at ASC";
            case TITLE_ASC -> "ORDER BY title ASC, created_at DESC";
        };
    }

    private SearchResultItemDto mapRowToDto(ResultSet rs) throws SQLException {
        String entityTypeStr = rs.getString("entity_type");
        UUID entityId = rs.getObject("entity_id", UUID.class);
        UUID dependentId = rs.getObject("dependent_id", UUID.class);
        String title = rs.getString("title");
        String subtitle = rs.getString("subtitle");
        String category = rs.getString("category_or_type");
        String status = rs.getString("status");
        BigDecimal amount = rs.getBigDecimal("amount");
        String currency = rs.getString("currency");
        LocalDate eventDate = rs.getObject("event_date", LocalDate.class);
        double relevanceScore = rs.getDouble("relevance_score");
        String snippet = rs.getString("snippet");

        Timestamp createdAtTs = rs.getTimestamp("created_at");
        OffsetDateTime createdAt = (createdAtTs != null)
                ? createdAtTs.toInstant().atZone(ZoneOffset.UTC).toOffsetDateTime()
                : null;

        SearchEntityType entityType = SearchEntityType.valueOf(entityTypeStr);

        return SearchResultItemDto.builder()
                .entityType(entityType)
                .entityId(entityId)
                .title(title)
                .subtitle(subtitle)
                .snippet(snippet)
                .category(category)
                .status(status)
                .amount(amount)
                .currency(currency)
                .eventDate(eventDate)
                .dependentId(dependentId)
                .entityRoute(resolveEntityRoute(entityType, entityId))
                .relevanceScore(Math.round(relevanceScore * 1000.0) / 1000.0)
                .createdAt(createdAt)
                .metadata(new HashMap<>())
                .build();
    }

    private String resolveEntityRoute(SearchEntityType type, UUID entityId) {
        if (type == null || entityId == null) {
            return "/";
        }
        return switch (type) {
            case ASSET -> "/assets/" + entityId;
            case INVOICE -> "/invoices/" + entityId;
            case WARRANTY -> "/warranties/" + entityId;
            case WARRANTY_CLAIM -> "/warranty-claims/" + entityId;
            case SERVICE_RECORD -> "/asset-service-records/" + entityId;
            case LOAN -> "/loans/" + entityId;
            case INSURANCE_POLICY -> "/insurance/" + entityId;
            case HEALTH_APPOINTMENT -> "/healthcare/appointments/" + entityId;
            case TRIP -> "/travel/trips/" + entityId;
            case ITINERARY_ITEM -> "/travel/itinerary-items/" + entityId;
            case FINANCE_TRANSACTION -> "/finance/transactions/" + entityId;
            case BUDGET -> "/budgets/" + entityId;
            case DOCUMENT -> "/documents/" + entityId;
            case DEPENDENT -> "/dependents/" + entityId;
            case REMINDER -> "/reminders/" + entityId;
        };
    }
}
