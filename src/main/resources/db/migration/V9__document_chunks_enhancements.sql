-- ==============================================================================
-- Flyway Migration: V9__document_chunks_enhancements.sql
-- Subsystem: Phase 11 - Document Intelligence & RAG Ingestion Foundation
-- Target: PostgreSQL 16/18 with pgvector & tsvector
-- ==============================================================================

-- 1. Enhance document_chunks with versioning, structure, and active state
ALTER TABLE document_chunks
    ADD COLUMN IF NOT EXISTS document_version INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS section_title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS token_count INT,
    ADD COLUMN IF NOT EXISTS char_count INT,
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100) DEFAULT 'text-embedding-3-small',
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- 2. Uniqueness constraint for idempotent chunking per document, version, and index
ALTER TABLE document_chunks
    ADD CONSTRAINT uq_document_chunks_doc_ver_idx 
    UNIQUE (document_id, document_version, chunk_index);

-- 3. Optimized index for active chunk filtering and version scoping
CREATE INDEX IF NOT EXISTS idx_chunks_active_user 
    ON document_chunks(user_id, is_active) 
    WHERE is_active;

CREATE INDEX IF NOT EXISTS idx_chunks_doc_ver 
    ON document_chunks(document_id, document_version);

-- 4. Track chunk metrics and embedding status directly on documents table
ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS chunk_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS ingested_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100);
