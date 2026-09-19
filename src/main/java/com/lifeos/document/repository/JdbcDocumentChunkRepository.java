package com.lifeos.document.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * High-performance JDBC repository for batch vector chunk persistence and versioning.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JdbcDocumentChunkRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Batch inserts document chunks with pgvector embeddings into PostgreSQL.
     */
    public void batchInsertChunks(List<ChunkInsertDto> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO document_chunks (" +
                "id, document_id, user_id, document_version, chunk_index, " +
                "content, embedding, page_number, section_title, " +
                "token_count, char_count, is_active, embedding_model, metadata, " +
                "created_at, updated_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?::vector, ?, ?, ?, ?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ChunkInsertDto chunk = chunks.get(i);
                ps.setObject(1, chunk.getId());
                ps.setObject(2, chunk.getDocumentId());
                ps.setObject(3, chunk.getUserId());
                ps.setInt(4, chunk.getDocumentVersion());
                ps.setInt(5, chunk.getChunkIndex());
                ps.setString(6, chunk.getContent());
                ps.setString(7, formatVector(chunk.getEmbedding()));
                ps.setInt(8, chunk.getPageNumber());
                ps.setString(9, chunk.getSectionTitle());
                ps.setInt(10, chunk.getTokenCount());
                ps.setInt(11, chunk.getCharCount());
                ps.setBoolean(12, chunk.isActive());
                ps.setString(13, chunk.getEmbeddingModel());
                ps.setString(14, formatJson(chunk.getMetadata()));
            }

            @Override
            public int getBatchSize() {
                return chunks.size();
            }
        });

        log.info("Batch inserted {} chunks for document [{}] v{}",
                chunks.size(), chunks.getFirst().getDocumentId(), chunks.getFirst().getDocumentVersion());
    }

    /**
     * Deactivates all chunks belonging to older versions of a document.
     */
    public int deactivateOldVersions(UUID documentId, int newVersion) {
        String sql = "UPDATE document_chunks SET is_active = false, updated_at = CURRENT_TIMESTAMP " +
                "WHERE document_id = ? AND document_version < ?";
        return jdbcTemplate.update(sql, documentId, newVersion);
    }

    /**
     * Deletes all chunks for a specific document and version (idempotent reprocess cleanup).
     */
    public int deleteChunksByDocumentAndVersion(UUID documentId, int version) {
        String sql = "DELETE FROM document_chunks WHERE document_id = ? AND document_version = ?";
        return jdbcTemplate.update(sql, documentId, version);
    }

    /**
     * Deletes all chunks for a document (used during document deletion).
     */
    public int deleteAllChunksByDocument(UUID documentId) {
        String sql = "DELETE FROM document_chunks WHERE document_id = ?";
        return jdbcTemplate.update(sql, documentId);
    }

    private String formatVector(float[] vector) {
        if (vector == null || vector.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            return "{}";
        }
    }
}
