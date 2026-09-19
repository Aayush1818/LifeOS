-- ============================================================================
-- Flyway Migration V4: Loan Amortization & Insurance Management Enhancements
-- ============================================================================

-- 1. Enhance loans table
ALTER TABLE loans
    ADD COLUMN IF NOT EXISTS interest_type VARCHAR(30) NOT NULL DEFAULT 'FIXED',
    ADD COLUMN IF NOT EXISTS payment_frequency VARCHAR(30) NOT NULL DEFAULT 'MONTHLY',
    ADD COLUMN IF NOT EXISTS total_principal_paid NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS total_interest_paid NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS notes TEXT;

CREATE INDEX IF NOT EXISTS idx_loans_user_status ON loans(user_id, status) WHERE NOT is_deleted;
CREATE INDEX IF NOT EXISTS idx_loans_user_lender ON loans(user_id, lender_name) WHERE NOT is_deleted;

-- 2. Enhance loan_payments table
ALTER TABLE loan_payments
    ADD COLUMN IF NOT EXISTS payment_type VARCHAR(30) NOT NULL DEFAULT 'REGULAR_EMI',
    ADD COLUMN IF NOT EXISTS notes TEXT;

CREATE INDEX IF NOT EXISTS idx_loan_payments_date ON loan_payments(loan_id, payment_date);

-- 3. Enhance insurance_policies table
ALTER TABLE insurance_policies
    ADD COLUMN IF NOT EXISTS policy_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS metadata JSONB;

CREATE INDEX IF NOT EXISTS idx_insurance_user_status ON insurance_policies(user_id, status) WHERE NOT is_deleted;
CREATE INDEX IF NOT EXISTS idx_insurance_renewal_window ON insurance_policies(user_id, next_renewal_date) WHERE status = 'ACTIVE' AND NOT is_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS uq_insurance_user_policy_no ON insurance_policies(user_id, policy_number) WHERE NOT is_deleted;

-- 4. Enhance reminders table
ALTER TABLE reminders
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_reminders_target_entity ON reminders(target_entity_id);
CREATE INDEX IF NOT EXISTS idx_reminders_user_status ON reminders(user_id, status) WHERE NOT is_deleted;
