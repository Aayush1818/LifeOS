-- ============================================================================
-- LifeOS Migration V11: Agent Tools & Pending Actions (Phase 14)
-- ============================================================================

CREATE TABLE IF NOT EXISTS pending_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
    tool_name VARCHAR(100) NOT NULL,
    parameters JSONB NOT NULL DEFAULT '{}'::jsonb,
    prompt TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    executed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_pending_action_status CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_pending_actions_user_status 
    ON pending_actions(user_id, status);

CREATE INDEX IF NOT EXISTS idx_pending_actions_expires 
    ON pending_actions(expires_at) WHERE status = 'PENDING';
