-- ============================================================================
-- LifeOS Database Migration: V2__document_enhancements.sql
-- Target: PostgreSQL 16/18 with pgvector & tsvector
-- Adds versioning, extracted text, metadata JSONB, and status indexing to documents
-- ============================================================================

ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS extracted_text TEXT,
    ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS extraction_error TEXT;

CREATE INDEX IF NOT EXISTS idx_documents_user_dep ON documents(user_id, dependent_id) WHERE NOT is_deleted;
CREATE INDEX IF NOT EXISTS idx_documents_user_status ON documents(user_id, ingestion_status) WHERE NOT is_deleted;
