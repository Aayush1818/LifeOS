-- ============================================================================
-- LifeOS Flyway Migration: V3__finance_enhancements.sql
-- Personal Finance, Recurring Transactions, and Monthly Budgeting Enhancements
-- ============================================================================

-- 1. Enhance transactions table with monetary precision, status, notes, receipt and recurring links
ALTER TABLE transactions
    ALTER COLUMN amount TYPE NUMERIC(14,2),
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'POSTED',
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS document_id UUID REFERENCES documents(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS recurring_id UUID,
    ADD COLUMN IF NOT EXISTS is_refund BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 2. Create recurring_transactions table
CREATE TABLE IF NOT EXISTS recurring_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL, -- INCOME, EXPENSE
    category VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    recurrence_pattern VARCHAR(50) NOT NULL, -- DAILY, WEEKLY, MONTHLY, QUARTERLY, ANNUALLY
    billing_day INT NOT NULL CHECK (billing_day BETWEEN 1 AND 31),
    start_date DATE NOT NULL,
    end_date DATE,
    next_due_date DATE NOT NULL,
    last_generated_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, PAUSED, COMPLETED
    auto_create_transaction BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Foreign key link from transactions to recurring_transactions
ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_recurring
    FOREIGN KEY (recurring_id) REFERENCES recurring_transactions(id) ON DELETE SET NULL;

-- 3. Enhance budgets table
ALTER TABLE budgets
    ALTER COLUMN allocated_amount TYPE NUMERIC(14,2),
    ADD COLUMN IF NOT EXISTS alert_thresholds JSONB NOT NULL DEFAULT '[50, 75, 90, 100]'::jsonb,
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 4. High-Performance Query & Aggregation Indexes
CREATE INDEX IF NOT EXISTS idx_transactions_user_date_status
    ON transactions(user_id, transaction_date, status) WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_transactions_user_cat_date
    ON transactions(user_id, category, transaction_date) WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_recurring_user_next_due
    ON recurring_transactions(user_id, next_due_date) WHERE status = 'ACTIVE' AND NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_budgets_user_period
    ON budgets(user_id, budget_year, budget_month);
