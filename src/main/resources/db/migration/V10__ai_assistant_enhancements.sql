-- ============================================================================
-- LifeOS Migration V10: AI Assistant Conversation & Citation Enhancements
-- ============================================================================

-- 1. Index for fast paginated sidebar conversation loading ordered by recency
CREATE INDEX IF NOT EXISTS idx_conversations_user_updated 
    ON conversations(user_id, last_message_at DESC);

-- 2. Telemetry and model metadata columns on chat_messages
ALTER TABLE chat_messages
    ADD COLUMN IF NOT EXISTS prompt_tokens INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS completion_tokens INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS model_name VARCHAR(100);

-- 3. Provenance and display metadata columns on message_citations
ALTER TABLE message_citations
    ADD COLUMN IF NOT EXISTS citation_index INT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS section_title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS source_citation VARCHAR(500);
