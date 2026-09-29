-- =====================================================================
-- Migration V14: Proactive Life Insights, Anomaly Detection & Cross-Domain Optimizations
-- =====================================================================

CREATE TABLE IF NOT EXISTS insights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    insight_type VARCHAR(60) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    action_type VARCHAR(50),
    action_payload JSONB DEFAULT '{}',
    is_dismissed BOOLEAN NOT NULL DEFAULT FALSE,
    is_actioned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Index for retrieving non-dismissed active insights per user
CREATE INDEX IF NOT EXISTS idx_insights_user_active
    ON insights (user_id, is_dismissed, created_at DESC)
    WHERE is_deleted = FALSE;

-- Index for filtering by insight type and severity
CREATE INDEX IF NOT EXISTS idx_insights_user_type_severity
    ON insights (user_id, insight_type, severity)
    WHERE is_deleted = FALSE;
