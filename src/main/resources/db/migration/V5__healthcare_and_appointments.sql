-- ============================================================================
-- V5: Healthcare Appointments & Medical Document Organization Enhancements
-- ============================================================================

-- 1. Enhance health_appointments with clinic contact details, follow-ups, and timezone tracking
ALTER TABLE health_appointments
    ADD COLUMN IF NOT EXISTS clinic_phone VARCHAR(30),
    ADD COLUMN IF NOT EXISTS clinic_address VARCHAR(255),
    ADD COLUMN IF NOT EXISTS follow_up_to_id UUID REFERENCES health_appointments(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS scheduled_end_time TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS time_zone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    ADD COLUMN IF NOT EXISTS reminder_offset_minutes INT NOT NULL DEFAULT 1440,
    ADD COLUMN IF NOT EXISTS metadata JSONB NOT NULL DEFAULT '{}'::jsonb;

-- 2. Multi-tenant and temporal query indexes
CREATE INDEX IF NOT EXISTS idx_health_user_status_time 
    ON health_appointments(user_id, status, appointment_time) 
    WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_health_user_dependent 
    ON health_appointments(user_id, dependent_id) 
    WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_health_follow_up 
    ON health_appointments(follow_up_to_id) 
    WHERE follow_up_to_id IS NOT NULL;

-- 3. Document entity links composite index and uniqueness
CREATE INDEX IF NOT EXISTS idx_doc_entity_links_composite 
    ON document_entity_links(entity_type, entity_id, document_id);

ALTER TABLE document_entity_links 
    ADD CONSTRAINT uq_doc_entity_links UNIQUE (entity_type, entity_id, document_id);
