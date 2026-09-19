package com.lifeos.rag.retrieval.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.rag.retrieval.dto.RetrievalFilters;
import com.lifeos.rag.retrieval.dto.RetrievalMatchSource;
import com.lifeos.rag.retrieval.model.RetrievalCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * High-performance JDBC repository executing tenant-scoped lexical FTS and
 * pgvector HNSW candidate queries directly over PostgreSQL.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JdbcRetrievalRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Executes PostgreSQL full-text search over document_chunks with metadata filtering and tenant isolation.
     */
    public List<RetrievalCandidate> findLexicalCandidates(UUID userId, String queryText, RetrievalFilters filters, int limit) {
        if (queryText == null || queryText.trim().isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("queryText", queryText.trim());
        params.addValue("likePattern", "%" + queryText.trim() + "%");
        params.addValue("limit", limit);

        StringBuilder sql = new StringBuilder("""
            SELECT c.id AS chunk_id, c.document_id, c.document_version, c.chunk_index, c.user_id,
                   c.content, c.page_number, c.section_title, c.token_count, c.metadata,
                   d.title AS doc_title, d.original_filename, d.category AS doc_category, d.document_type, d.dependent_id,
                   ts_rank_cd(c.tsv_content, websearch_to_tsquery('english', :queryText), 32) AS lexical_score,
                   ts_headline('english', c.content, websearch_to_tsquery('english', :queryText),
                               'StartSel=<b>, StopSel=</b>, MaxWords=35, MinWords=15') AS snippet
            FROM document_chunks c
            JOIN documents d ON d.id = c.document_id
            WHERE c.user_id = :userId
              AND d.user_id = :userId
              AND d.is_deleted = false
              AND (c.tsv_content @@ websearch_to_tsquery('english', :queryText) OR d.title ILIKE :likePattern)
        """);

        applyFilters(filters, params, sql);

        sql.append(" ORDER BY lexical_score DESC, c.chunk_index ASC LIMIT :limit");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            RetrievalCandidate candidate = mapRowToCandidate(rs);
            candidate.setLexicalScore(rs.getDouble("lexical_score"));
            candidate.setLexicalRank(rowNum + 1);
            candidate.setSnippet(rs.getString("snippet"));
            candidate.setMatchSource(RetrievalMatchSource.LEXICAL_ONLY);
            return candidate;
        });
    }

    /**
     * Executes pgvector cosine distance search over document_chunks with metadata filtering and tenant isolation.
     */
    public List<RetrievalCandidate> findVectorCandidates(
            UUID userId, float[] queryEmbedding, RetrievalFilters filters, int limit, double minSimilarityThreshold) {
        if (queryEmbedding == null || queryEmbedding.length == 0 || limit <= 0) {
            return Collections.emptyList();
        }

        String vectorStr = formatVector(queryEmbedding);
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("vectorStr", vectorStr);
        params.addValue("minSimilarity", minSimilarityThreshold);
        params.addValue("limit", limit);

        StringBuilder sql = new StringBuilder("""
            SELECT c.id AS chunk_id, c.document_id, c.document_version, c.chunk_index, c.user_id,
                   c.content, c.page_number, c.section_title, c.token_count, c.metadata,
                   d.title AS doc_title, d.original_filename, d.category AS doc_category, d.document_type, d.dependent_id,
                   SUBSTRING(c.content FROM 1 FOR 200) AS snippet,
                   (1.0 - (c.embedding <=> CAST(:vectorStr AS vector))) AS vector_similarity
            FROM document_chunks c
            JOIN documents d ON d.id = c.document_id
            WHERE c.user_id = :userId
              AND d.user_id = :userId
              AND d.is_deleted = false
        """);

        if (minSimilarityThreshold > -0.99) {
            params.addValue("minSimilarity", minSimilarityThreshold);
            sql.append(" AND (1.0 - (c.embedding <=> CAST(:vectorStr AS vector))) >= :minSimilarity");
        }

        applyFilters(filters, params, sql);

        sql.append(" ORDER BY (c.embedding <=> CAST(:vectorStr AS vector)) ASC LIMIT :limit");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            RetrievalCandidate candidate = mapRowToCandidate(rs);
            double sim = rs.getDouble("vector_similarity");
            candidate.setVectorScore(Math.round(sim * 1000.0) / 1000.0);
            candidate.setVectorRank(rowNum + 1);
            candidate.setSnippet(rs.getString("snippet"));
            candidate.setMatchSource(RetrievalMatchSource.SEMANTIC_ONLY);
            return candidate;
        });
    }

    private void applyFilters(RetrievalFilters filters, MapSqlParameterSource params, StringBuilder sql) {
        if (filters == null || !Boolean.TRUE.equals(filters.getIncludeHistorical())) {
            sql.append(" AND c.is_active = true");
        }

        if (filters == null) {
            return;
        }

        if (filters.getCategory() != null) {
            params.addValue("category", filters.getCategory().name());
            sql.append(" AND d.category = :category");
        }

        if (filters.getDocumentType() != null) {
            params.addValue("documentType", filters.getDocumentType().name());
            sql.append(" AND d.document_type = :documentType");
        }

        if (filters.getDependentId() != null) {
            params.addValue("dependentId", filters.getDependentId());
            sql.append(" AND d.dependent_id = :dependentId");
        }

        if (filters.getDocumentId() != null) {
            params.addValue("documentId", filters.getDocumentId());
            sql.append(" AND c.document_id = :documentId");
        }

        if (filters.getDocumentVersion() != null) {
            params.addValue("docVersion", filters.getDocumentVersion());
            sql.append(" AND c.document_version = :docVersion");
        }

        if (filters.getStartDate() != null) {
            OffsetDateTime startDt = filters.getStartDate().atStartOfDay().atOffset(ZoneOffset.UTC);
            params.addValue("startDate", filters.getStartDate());
            params.addValue("startDt", startDt);
            sql.append(" AND (d.issue_date >= :startDate OR (d.issue_date IS NULL AND d.created_at >= :startDt))");
        }

        if (filters.getEndDate() != null) {
            OffsetDateTime endDt = filters.getEndDate().atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);
            params.addValue("endDate", filters.getEndDate());
            params.addValue("endDt", endDt);
            sql.append(" AND (d.issue_date <= :endDate OR (d.issue_date IS NULL AND d.created_at <= :endDt))");
        }

        if (filters.getSection() != null && !filters.getSection().trim().isEmpty()) {
            params.addValue("sectionPattern", "%" + filters.getSection().trim() + "%");
            sql.append(" AND c.section_title ILIKE :sectionPattern");
        }

        if (filters.getPageNumber() != null) {
            params.addValue("pageNumber", filters.getPageNumber());
            sql.append(" AND c.page_number = :pageNumber");
        }

        if (filters.getMimeType() != null && !filters.getMimeType().trim().isEmpty()) {
            params.addValue("mimeType", filters.getMimeType().trim());
            sql.append(" AND d.mime_type = :mimeType");
        }

        if (filters.getEntityType() != null && filters.getEntityId() != null) {
            params.addValue("entityType", filters.getEntityType().trim());
            params.addValue("entityId", filters.getEntityId());
            sql.append("""
                 AND EXISTS (
                     SELECT 1 FROM document_entity_links del
                     WHERE del.document_id = d.id
                       AND del.entity_type = :entityType
                       AND del.entity_id = :entityId
                 )
            """);
        }
    }

    private RetrievalCandidate mapRowToCandidate(ResultSet rs) throws SQLException {
        UUID chunkId = rs.getObject("chunk_id", UUID.class);
        UUID documentId = rs.getObject("document_id", UUID.class);
        Integer documentVersion = rs.getInt("document_version");
        Integer chunkIndex = rs.getInt("chunk_index");
        UUID userId = rs.getObject("user_id", UUID.class);
        String content = rs.getString("content");
        Integer pageNumber = rs.getInt("page_number");
        String sectionTitle = rs.getString("section_title");
        Integer tokenCount = rs.getInt("token_count");

        String docTitle = rs.getString("doc_title");
        String originalFilename = rs.getString("original_filename");
        String docCategory = rs.getString("doc_category");
        String documentType = rs.getString("document_type");
        UUID dependentId = rs.getObject("dependent_id", UUID.class);

        String metadataJson = rs.getString("metadata");
        Map<String, Object> metadata = parseJson(metadataJson);

        return RetrievalCandidate.builder()
                .chunkId(chunkId)
                .documentId(documentId)
                .documentVersion(documentVersion)
                .chunkIndex(chunkIndex)
                .userId(userId)
                .content(content)
                .pageNumber(pageNumber)
                .sectionTitle(sectionTitle)
                .tokenCount(tokenCount)
                .documentTitle(docTitle)
                .originalFilename(originalFilename)
                .documentCategory(docCategory)
                .documentType(documentType)
                .dependentId(dependentId)
                .metadata(metadata)
                .build();
    }

    private String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
