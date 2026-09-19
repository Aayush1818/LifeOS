-- ==============================================================================
-- Flyway Migration: V7__assets_warranties_and_invoices.sql
-- Subsystem: Phase 9 - Product Warranties, Invoices & Asset Management
-- ==============================================================================

-- 1. Invoices Table (First-Class Domain Entity)
CREATE TABLE IF NOT EXISTS invoices (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invoice_number    VARCHAR(100) NOT NULL,
    vendor_name       VARCHAR(255) NOT NULL,
    invoice_date      DATE NOT NULL,
    due_date          DATE,
    return_deadline   DATE,
    currency          VARCHAR(3) NOT NULL DEFAULT 'USD',
    subtotal          NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    tax_amount        NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    discount_amount   NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    shipping_amount   NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    other_charges     NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    total_amount      NUMERIC(14, 2) NOT NULL,
    payment_status    VARCHAR(50) NOT NULL DEFAULT 'PAID',
    payment_date      DATE,
    payment_method    VARCHAR(50),
    transaction_id    UUID UNIQUE REFERENCES transactions(id) ON DELETE SET NULL,
    document_id       UUID REFERENCES documents(id) ON DELETE SET NULL,
    notes             TEXT,
    is_deleted        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_invoice_currency_len CHECK (char_length(currency) = 3),
    CONSTRAINT chk_invoice_total_positive CHECK (total_amount >= 0.00)
);

CREATE INDEX IF NOT EXISTS idx_invoices_user_date ON invoices(user_id, invoice_date DESC) WHERE NOT is_deleted;
CREATE INDEX IF NOT EXISTS idx_invoices_user_vendor ON invoices(user_id, vendor_name) WHERE NOT is_deleted;
CREATE INDEX IF NOT EXISTS idx_invoices_transaction_id ON invoices(transaction_id) WHERE transaction_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_invoices_user_deleted ON invoices(user_id, is_deleted);

-- 2. Assets Table
CREATE TABLE IF NOT EXISTS assets (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    dependent_id       UUID REFERENCES dependents(id) ON DELETE SET NULL,
    name               VARCHAR(255) NOT NULL,
    category           VARCHAR(50) NOT NULL,
    brand              VARCHAR(100),
    model_number       VARCHAR(100),
    serial_number      VARCHAR(100),
    purchase_date      DATE,
    return_deadline    DATE,
    purchase_price     NUMERIC(14, 2),
    currency           VARCHAR(3) NOT NULL DEFAULT 'USD',
    primary_invoice_id UUID REFERENCES invoices(id) ON DELETE SET NULL,
    location           VARCHAR(100),
    status             VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    notes              TEXT,
    is_deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_assets_currency_len CHECK (char_length(currency) = 3)
);

CREATE INDEX IF NOT EXISTS idx_assets_user_status ON assets(user_id, status) WHERE NOT is_deleted;
CREATE INDEX IF NOT EXISTS idx_assets_user_category ON assets(user_id, category) WHERE NOT is_deleted;
CREATE INDEX IF NOT EXISTS idx_assets_dependent_id ON assets(dependent_id) WHERE dependent_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_assets_serial_number ON assets(user_id, serial_number) WHERE serial_number IS NOT NULL AND NOT is_deleted;
CREATE INDEX IF NOT EXISTS idx_assets_user_deleted ON assets(user_id, is_deleted);

-- 3. Invoice Items Table
CREATE TABLE IF NOT EXISTS invoice_items (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id        UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    asset_id          UUID REFERENCES assets(id) ON DELETE SET NULL,
    item_description  VARCHAR(255) NOT NULL,
    quantity          INTEGER NOT NULL DEFAULT 1,
    unit_price        NUMERIC(14, 2) NOT NULL,
    total_price       NUMERIC(14, 2) NOT NULL,
    notes             TEXT,
    is_deleted        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_invoice_item_qty CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice_id ON invoice_items(invoice_id) WHERE NOT is_deleted;
CREATE INDEX IF NOT EXISTS idx_invoice_items_asset_id ON invoice_items(asset_id) WHERE asset_id IS NOT NULL;

-- 4. Warranties Table
CREATE TABLE IF NOT EXISTS warranties (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    asset_id             UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    provider             VARCHAR(255) NOT NULL,
    warranty_type        VARCHAR(50) NOT NULL,
    policy_number        VARCHAR(100),
    start_date           DATE NOT NULL,
    expiry_date          DATE,
    status               VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    coverage_details     TEXT,
    deductible_amount    NUMERIC(14, 2),
    currency             VARCHAR(3) NOT NULL DEFAULT 'USD',
    reminder_id          UUID REFERENCES reminders(id) ON DELETE SET NULL,
    reminder_offset_days INTEGER NOT NULL DEFAULT 30,
    contact_phone        VARCHAR(50),
    contact_email        VARCHAR(255),
    notes                TEXT,
    is_deleted           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_warranty_currency_len CHECK (char_length(currency) = 3)
);

CREATE INDEX IF NOT EXISTS idx_warranties_user_asset ON warranties(user_id, asset_id) WHERE NOT is_deleted;
CREATE INDEX IF NOT EXISTS idx_warranties_user_expiry ON warranties(user_id, expiry_date) WHERE NOT is_deleted;
CREATE INDEX IF NOT EXISTS idx_warranties_status ON warranties(status) WHERE NOT is_deleted;
CREATE INDEX IF NOT EXISTS idx_warranties_reminder_id ON warranties(reminder_id) WHERE reminder_id IS NOT NULL;

-- 5. Warranty Claims Table
CREATE TABLE IF NOT EXISTS warranty_claims (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    warranty_id         UUID NOT NULL REFERENCES warranties(id) ON DELETE CASCADE,
    asset_id            UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    claim_number        VARCHAR(100),
    claim_date          DATE NOT NULL,
    claim_type          VARCHAR(50) NOT NULL,
    status              VARCHAR(50) NOT NULL DEFAULT 'FILED',
    description         TEXT NOT NULL,
    resolution          TEXT,
    resolved_date       DATE,
    claim_cost_covered  NUMERIC(14, 2),
    out_of_pocket_cost  NUMERIC(14, 2),
    currency            VARCHAR(3) NOT NULL DEFAULT 'USD',
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_claim_currency_len CHECK (char_length(currency) = 3)
);

CREATE INDEX IF NOT EXISTS idx_warranty_claims_user_warranty ON warranty_claims(user_id, warranty_id) WHERE NOT is_deleted;
CREATE INDEX IF NOT EXISTS idx_warranty_claims_user_asset ON warranty_claims(user_id, asset_id) WHERE NOT is_deleted;
CREATE INDEX IF NOT EXISTS idx_warranty_claims_status ON warranty_claims(status) WHERE NOT is_deleted;

-- 6. Asset Service Records Table
CREATE TABLE IF NOT EXISTS asset_service_records (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    asset_id           UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    service_date       DATE NOT NULL,
    service_type       VARCHAR(50) NOT NULL,
    service_provider   VARCHAR(255) NOT NULL,
    description        TEXT NOT NULL,
    cost               NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    currency           VARCHAR(3) NOT NULL DEFAULT 'USD',
    status             VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    invoice_id         UUID REFERENCES invoices(id) ON DELETE SET NULL,
    warranty_claim_id  UUID REFERENCES warranty_claims(id) ON DELETE SET NULL,
    notes              TEXT,
    is_deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_service_currency_len CHECK (char_length(currency) = 3)
);

CREATE INDEX IF NOT EXISTS idx_service_records_user_asset ON asset_service_records(user_id, asset_id) WHERE NOT is_deleted;
CREATE INDEX IF NOT EXISTS idx_service_records_date ON asset_service_records(service_date DESC) WHERE NOT is_deleted;

-- 7. Asset Status History Table (Immutable audit trail)
CREATE TABLE IF NOT EXISTS asset_status_history (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id     UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL,
    from_status  VARCHAR(50) NOT NULL,
    to_status    VARCHAR(50) NOT NULL,
    reason       TEXT,
    changed_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_asset_status_history_asset_id ON asset_status_history(asset_id, changed_at DESC);
