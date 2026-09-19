-- ==============================================================================
-- Flyway Migration: V8__unified_search_indexes.sql
-- Subsystem: Phase 10 - Unified Search & Advanced Query Platform
-- Target: PostgreSQL 16/18 with pgvector & tsvector
-- ==============================================================================

-- 1. Expression-Based Full-Text GIN Indexes on Existing Domain Tables
CREATE INDEX IF NOT EXISTS idx_documents_fts ON documents 
    USING gin (to_tsvector('english', coalesce(title, '') || ' ' || coalesce(original_filename, '') || ' ' || coalesce(substring(extracted_text, 1, 5000), '')))
    WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_assets_fts ON assets 
    USING gin (to_tsvector('english', coalesce(name, '') || ' ' || coalesce(brand, '') || ' ' || coalesce(model_number, '') || ' ' || coalesce(serial_number, '') || ' ' || coalesce(notes, '')))
    WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_invoices_fts ON invoices 
    USING gin (to_tsvector('english', coalesce(invoice_number, '') || ' ' || coalesce(vendor_name, '') || ' ' || coalesce(notes, '')))
    WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_warranties_fts ON warranties 
    USING gin (to_tsvector('english', coalesce(provider, '') || ' ' || coalesce(policy_number, '') || ' ' || coalesce(coverage_details, '') || ' ' || coalesce(notes, '')))
    WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_warranty_claims_fts ON warranty_claims 
    USING gin (to_tsvector('english', coalesce(claim_number, '') || ' ' || coalesce(description, '') || ' ' || coalesce(resolution, '')))
    WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_service_records_fts ON asset_service_records 
    USING gin (to_tsvector('english', coalesce(service_provider, '') || ' ' || coalesce(description, '') || ' ' || coalesce(notes, '')))
    WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_appointments_fts ON health_appointments 
    USING gin (to_tsvector('english', coalesce(doctor_name, '') || ' ' || coalesce(specialization, '') || ' ' || coalesce(clinic_or_hospital, '') || ' ' || coalesce(purpose, '') || ' ' || coalesce(notes, '')))
    WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_trips_fts ON trips 
    USING gin (to_tsvector('english', coalesce(trip_title, '') || ' ' || coalesce(destination, '') || ' ' || coalesce(notes, '')))
    WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_itinerary_items_fts ON itinerary_items 
    USING gin (to_tsvector('english', coalesce(title, '') || ' ' || coalesce(confirmation_details, '') || ' ' || coalesce(start_location, '') || ' ' || coalesce(end_location, '') || ' ' || coalesce(booking_reference, '') || ' ' || coalesce(provider, '') || ' ' || coalesce(notes, '')))
    WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_transactions_fts ON transactions 
    USING gin (to_tsvector('english', coalesce(description, '') || ' ' || coalesce(notes, '')))
    WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_loans_fts ON loans 
    USING gin (to_tsvector('english', coalesce(lender_name, '') || ' ' || coalesce(loan_account_number, '')))
    WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_insurance_policies_fts ON insurance_policies 
    USING gin (to_tsvector('english', coalesce(policy_name, '') || ' ' || coalesce(provider_name, '') || ' ' || coalesce(policy_number, '')))
    WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_dependents_fts ON dependents 
    USING gin (to_tsvector('english', coalesce(full_name, '') || ' ' || coalesce(relationship, '')));

CREATE INDEX IF NOT EXISTS idx_reminders_fts ON reminders 
    USING gin (to_tsvector('english', coalesce(title, '') || ' ' || coalesce(description, '')));

-- 2. Unified Search View over all 14 LifeOS domains
CREATE OR REPLACE VIEW lifeos_unified_search_view AS
    -- 1. ASSETS
    SELECT 
        'ASSET'::VARCHAR(50) AS entity_type,
        a.id AS entity_id,
        a.user_id AS user_id,
        a.dependent_id AS dependent_id,
        a.name::VARCHAR(255) AS title,
        (coalesce(a.brand, '') || CASE WHEN a.model_number IS NOT NULL THEN ' • ' || a.model_number ELSE '' END)::VARCHAR(255) AS subtitle,
        (coalesce(a.name, '') || ' ' || coalesce(a.brand, '') || ' ' || coalesce(a.model_number, '') || ' ' || coalesce(a.serial_number, '') || ' ' || coalesce(a.notes, ''))::TEXT AS content_text,
        a.category::VARCHAR(100) AS category_or_type,
        a.status::VARCHAR(50) AS status,
        a.purchase_price::NUMERIC(14,2) AS amount,
        a.currency::VARCHAR(3) AS currency,
        a.purchase_date AS event_date,
        a.created_at AS created_at,
        a.is_deleted AS is_deleted,
        (setweight(to_tsvector('english', coalesce(a.name, '') || ' ' || coalesce(a.serial_number, '')), 'A') || 
         setweight(to_tsvector('english', coalesce(a.brand, '') || ' ' || coalesce(a.model_number, '')), 'B') || 
         setweight(to_tsvector('english', coalesce(a.notes, '') || ' ' || coalesce(a.location, '')), 'C')) AS tsv_content
    FROM assets a

    UNION ALL

    -- 2. INVOICES
    SELECT 
        'INVOICE'::VARCHAR(50) AS entity_type,
        i.id AS entity_id,
        i.user_id AS user_id,
        NULL::UUID AS dependent_id,
        i.invoice_number::VARCHAR(255) AS title,
        i.vendor_name::VARCHAR(255) AS subtitle,
        (coalesce(i.invoice_number, '') || ' ' || coalesce(i.vendor_name, '') || ' ' || coalesce(i.notes, '') || ' ' || 
         coalesce((SELECT string_agg(ii.item_description, ' ') FROM invoice_items ii WHERE ii.invoice_id = i.id AND NOT ii.is_deleted), ''))::TEXT AS content_text,
        'INVOICE'::VARCHAR(100) AS category_or_type,
        i.payment_status::VARCHAR(50) AS status,
        i.total_amount::NUMERIC(14,2) AS amount,
        i.currency::VARCHAR(3) AS currency,
        i.invoice_date AS event_date,
        i.created_at AS created_at,
        i.is_deleted AS is_deleted,
        (setweight(to_tsvector('english', coalesce(i.invoice_number, '')), 'A') || 
         setweight(to_tsvector('english', coalesce(i.vendor_name, '')), 'B') || 
         setweight(to_tsvector('english', coalesce(i.notes, '') || ' ' || coalesce((SELECT string_agg(ii.item_description, ' ') FROM invoice_items ii WHERE ii.invoice_id = i.id AND NOT ii.is_deleted), '')), 'C')) AS tsv_content
    FROM invoices i

    UNION ALL

    -- 3. WARRANTIES
    SELECT 
        'WARRANTY'::VARCHAR(50) AS entity_type,
        w.id AS entity_id,
        w.user_id AS user_id,
        NULL::UUID AS dependent_id,
        (coalesce(w.provider, '') || ' ' || coalesce(w.warranty_type, ''))::VARCHAR(255) AS title,
        coalesce(w.policy_number, '')::VARCHAR(255) AS subtitle,
        (coalesce(w.provider, '') || ' ' || coalesce(w.policy_number, '') || ' ' || coalesce(w.coverage_details, '') || ' ' || coalesce(w.notes, ''))::TEXT AS content_text,
        w.warranty_type::VARCHAR(100) AS category_or_type,
        w.status::VARCHAR(50) AS status,
        w.deductible_amount::NUMERIC(14,2) AS amount,
        w.currency::VARCHAR(3) AS currency,
        w.start_date AS event_date,
        w.created_at AS created_at,
        w.is_deleted AS is_deleted,
        (setweight(to_tsvector('english', coalesce(w.provider, '') || ' ' || coalesce(w.policy_number, '')), 'A') || 
         setweight(to_tsvector('english', coalesce(w.warranty_type, '')), 'B') || 
         setweight(to_tsvector('english', coalesce(w.coverage_details, '') || ' ' || coalesce(w.notes, '')), 'C')) AS tsv_content
    FROM warranties w

    UNION ALL

    -- 4. WARRANTY CLAIMS
    SELECT 
        'WARRANTY_CLAIM'::VARCHAR(50) AS entity_type,
        wc.id AS entity_id,
        wc.user_id AS user_id,
        NULL::UUID AS dependent_id,
        coalesce(wc.claim_number, 'Claim on ' || wc.claim_date::text)::VARCHAR(255) AS title,
        (wc.claim_type || ' • ' || wc.status)::VARCHAR(255) AS subtitle,
        (coalesce(wc.claim_number, '') || ' ' || coalesce(wc.description, '') || ' ' || coalesce(wc.resolution, ''))::TEXT AS content_text,
        wc.claim_type::VARCHAR(100) AS category_or_type,
        wc.status::VARCHAR(50) AS status,
        (coalesce(wc.claim_cost_covered, 0.00) + coalesce(wc.out_of_pocket_cost, 0.00))::NUMERIC(14,2) AS amount,
        wc.currency::VARCHAR(3) AS currency,
        wc.claim_date AS event_date,
        wc.created_at AS created_at,
        wc.is_deleted AS is_deleted,
        (setweight(to_tsvector('english', coalesce(wc.claim_number, '')), 'A') || 
         setweight(to_tsvector('english', coalesce(wc.claim_type, '')), 'B') || 
         setweight(to_tsvector('english', coalesce(wc.description, '') || ' ' || coalesce(wc.resolution, '')), 'C')) AS tsv_content
    FROM warranty_claims wc

    UNION ALL

    -- 5. ASSET SERVICE RECORDS
    SELECT 
        'SERVICE_RECORD'::VARCHAR(50) AS entity_type,
        sr.id AS entity_id,
        sr.user_id AS user_id,
        NULL::UUID AS dependent_id,
        (sr.service_provider || ' - ' || sr.service_type)::VARCHAR(255) AS title,
        sr.description::VARCHAR(255) AS subtitle,
        (coalesce(sr.service_provider, '') || ' ' || coalesce(sr.description, '') || ' ' || coalesce(sr.notes, ''))::TEXT AS content_text,
        sr.service_type::VARCHAR(100) AS category_or_type,
        sr.status::VARCHAR(50) AS status,
        sr.cost::NUMERIC(14,2) AS amount,
        sr.currency::VARCHAR(3) AS currency,
        sr.service_date AS event_date,
        sr.created_at AS created_at,
        sr.is_deleted AS is_deleted,
        (setweight(to_tsvector('english', coalesce(sr.service_provider, '')), 'A') || 
         setweight(to_tsvector('english', coalesce(sr.service_type, '')), 'B') || 
         setweight(to_tsvector('english', coalesce(sr.description, '') || ' ' || coalesce(sr.notes, '')), 'C')) AS tsv_content
    FROM asset_service_records sr

    UNION ALL

    -- 6. LOANS
    SELECT 
        'LOAN'::VARCHAR(50) AS entity_type,
        l.id AS entity_id,
        l.user_id AS user_id,
        NULL::UUID AS dependent_id,
        (coalesce(l.lender_name, 'Loan') || ' (' || coalesce(l.loan_type, '') || ')')::VARCHAR(255) AS title,
        ('Acc: ' || coalesce(l.loan_account_number, 'N/A'))::VARCHAR(255) AS subtitle,
        (coalesce(l.lender_name, '') || ' ' || coalesce(l.loan_account_number, '') || ' ' || coalesce(l.loan_type, ''))::TEXT AS content_text,
        l.loan_type::VARCHAR(100) AS category_or_type,
        l.status::VARCHAR(50) AS status,
        l.outstanding_balance::NUMERIC(14,2) AS amount,
        'USD'::VARCHAR(3) AS currency,
        l.start_date AS event_date,
        l.created_at AS created_at,
        l.is_deleted AS is_deleted,
        (setweight(to_tsvector('english', coalesce(l.lender_name, '') || ' ' || coalesce(l.loan_account_number, '')), 'A') || 
         setweight(to_tsvector('english', coalesce(l.loan_type, '')), 'B')) AS tsv_content
    FROM loans l

    UNION ALL

    -- 7. INSURANCE POLICIES
    SELECT 
        'INSURANCE_POLICY'::VARCHAR(50) AS entity_type,
        ip.id AS entity_id,
        ip.user_id AS user_id,
        ip.dependent_id AS dependent_id,
        coalesce(ip.policy_name, ip.provider_name || ' ' || ip.policy_type)::VARCHAR(255) AS title,
        (coalesce(ip.provider_name, '') || ' • Policy: ' || coalesce(ip.policy_number, ''))::VARCHAR(255) AS subtitle,
        (coalesce(ip.policy_name, '') || ' ' || coalesce(ip.provider_name, '') || ' ' || coalesce(ip.policy_number, '') || ' ' || coalesce(ip.policy_type, ''))::TEXT AS content_text,
        ip.policy_type::VARCHAR(100) AS category_or_type,
        ip.status::VARCHAR(50) AS status,
        ip.coverage_amount::NUMERIC(14,2) AS amount,
        'USD'::VARCHAR(3) AS currency,
        ip.expiry_date AS event_date,
        ip.created_at AS created_at,
        ip.is_deleted AS is_deleted,
        (setweight(to_tsvector('english', coalesce(ip.policy_name, '') || ' ' || coalesce(ip.policy_number, '')), 'A') || 
         setweight(to_tsvector('english', coalesce(ip.provider_name, '') || ' ' || coalesce(ip.policy_type, '')), 'B')) AS tsv_content
    FROM insurance_policies ip

    UNION ALL

    -- 8. HEALTHCARE APPOINTMENTS
    SELECT 
        'HEALTH_APPOINTMENT'::VARCHAR(50) AS entity_type,
        ha.id AS entity_id,
        ha.user_id AS user_id,
        ha.dependent_id AS dependent_id,
        (ha.doctor_name || ' (' || coalesce(ha.specialization, 'Doctor') || ')')::VARCHAR(255) AS title,
        (coalesce(ha.clinic_or_hospital, '') || CASE WHEN ha.purpose IS NOT NULL THEN ' • ' || ha.purpose ELSE '' END)::VARCHAR(255) AS subtitle,
        (coalesce(ha.doctor_name, '') || ' ' || coalesce(ha.specialization, '') || ' ' || coalesce(ha.clinic_or_hospital, '') || ' ' || coalesce(ha.purpose, '') || ' ' || coalesce(ha.notes, ''))::TEXT AS content_text,
        coalesce(ha.specialization, 'GENERAL')::VARCHAR(100) AS category_or_type,
        ha.status::VARCHAR(50) AS status,
        NULL::NUMERIC(14,2) AS amount,
        NULL::VARCHAR(3) AS currency,
        ha.appointment_time::DATE AS event_date,
        ha.created_at AS created_at,
        ha.is_deleted AS is_deleted,
        (setweight(to_tsvector('english', coalesce(ha.doctor_name, '') || ' ' || coalesce(ha.specialization, '')), 'A') || 
         setweight(to_tsvector('english', coalesce(ha.clinic_or_hospital, '') || ' ' || coalesce(ha.purpose, '')), 'B') || 
         setweight(to_tsvector('english', coalesce(ha.notes, '')), 'C')) AS tsv_content
    FROM health_appointments ha

    UNION ALL

    -- 9. TRIPS
    SELECT 
        'TRIP'::VARCHAR(50) AS entity_type,
        tr.id AS entity_id,
        tr.user_id AS user_id,
        NULL::UUID AS dependent_id,
        tr.trip_title::VARCHAR(255) AS title,
        tr.destination::VARCHAR(255) AS subtitle,
        (coalesce(tr.trip_title, '') || ' ' || coalesce(tr.destination, '') || ' ' || coalesce(tr.notes, ''))::TEXT AS content_text,
        'TRIP'::VARCHAR(100) AS category_or_type,
        tr.status::VARCHAR(50) AS status,
        tr.total_budget::NUMERIC(14,2) AS amount,
        tr.currency::VARCHAR(3) AS currency,
        tr.start_date AS event_date,
        tr.created_at AS created_at,
        tr.is_deleted AS is_deleted,
        (setweight(to_tsvector('english', coalesce(tr.trip_title, '') || ' ' || coalesce(tr.destination, '')), 'A') || 
         setweight(to_tsvector('english', coalesce(tr.notes, '')), 'C')) AS tsv_content
    FROM trips tr

    UNION ALL

    -- 10. ITINERARY ITEMS
    SELECT 
        'ITINERARY_ITEM'::VARCHAR(50) AS entity_type,
        ii.id AS entity_id,
        ii.user_id AS user_id,
        NULL::UUID AS dependent_id,
        ii.title::VARCHAR(255) AS title,
        (coalesce(ii.start_location, '') || CASE WHEN ii.provider IS NOT NULL THEN ' • ' || ii.provider ELSE '' END)::VARCHAR(255) AS subtitle,
        (coalesce(ii.title, '') || ' ' || coalesce(ii.confirmation_details, '') || ' ' || coalesce(ii.start_location, '') || ' ' || coalesce(ii.end_location, '') || ' ' || coalesce(ii.booking_reference, '') || ' ' || coalesce(ii.provider, '') || ' ' || coalesce(ii.notes, ''))::TEXT AS content_text,
        ii.item_type::VARCHAR(100) AS category_or_type,
        ii.status::VARCHAR(50) AS status,
        ii.cost::NUMERIC(14,2) AS amount,
        ii.currency::VARCHAR(3) AS currency,
        ii.start_time::DATE AS event_date,
        ii.created_at AS created_at,
        ii.is_deleted AS is_deleted,
        (setweight(to_tsvector('english', coalesce(ii.title, '') || ' ' || coalesce(ii.booking_reference, '')), 'A') || 
         setweight(to_tsvector('english', coalesce(ii.start_location, '') || ' ' || coalesce(ii.provider, '') || ' ' || coalesce(ii.item_type, '')), 'B') || 
         setweight(to_tsvector('english', coalesce(ii.confirmation_details, '') || ' ' || coalesce(ii.notes, '')), 'C')) AS tsv_content
    FROM itinerary_items ii

    UNION ALL

    -- 11. FINANCE TRANSACTIONS
    SELECT 
        'FINANCE_TRANSACTION'::VARCHAR(50) AS entity_type,
        tx.id AS entity_id,
        tx.user_id AS user_id,
        NULL::UUID AS dependent_id,
        coalesce(tx.description, tx.category)::VARCHAR(255) AS title,
        (tx.category || ' • ' || tx.payment_method)::VARCHAR(255) AS subtitle,
        (coalesce(tx.description, '') || ' ' || coalesce(tx.category, '') || ' ' || coalesce(tx.notes, ''))::TEXT AS content_text,
        tx.category::VARCHAR(100) AS category_or_type,
        tx.status::VARCHAR(50) AS status,
        tx.amount::NUMERIC(14,2) AS amount,
        'USD'::VARCHAR(3) AS currency,
        tx.transaction_date AS event_date,
        tx.created_at AS created_at,
        tx.is_deleted AS is_deleted,
        (setweight(to_tsvector('english', coalesce(tx.description, '')), 'A') || 
         setweight(to_tsvector('english', coalesce(tx.category, '') || ' ' || coalesce(tx.payment_method, '')), 'B') || 
         setweight(to_tsvector('english', coalesce(tx.notes, '')), 'C')) AS tsv_content
    FROM transactions tx

    UNION ALL

    -- 12. BUDGETS
    SELECT 
        'BUDGET'::VARCHAR(50) AS entity_type,
        b.id AS entity_id,
        b.user_id AS user_id,
        NULL::UUID AS dependent_id,
        ('Budget: ' || b.category)::VARCHAR(255) AS title,
        (b.budget_month::text || '/' || b.budget_year::text)::VARCHAR(255) AS subtitle,
        ('Budget for ' || b.category || ' ' || b.budget_month::text || '/' || b.budget_year::text)::TEXT AS content_text,
        b.category::VARCHAR(100) AS category_or_type,
        'ACTIVE'::VARCHAR(50) AS status,
        b.allocated_amount::NUMERIC(14,2) AS amount,
        'USD'::VARCHAR(3) AS currency,
        make_date(b.budget_year, b.budget_month, 1) AS event_date,
        b.created_at AS created_at,
        b.is_deleted AS is_deleted,
        setweight(to_tsvector('english', coalesce(b.category, '')), 'A') AS tsv_content
    FROM budgets b

    UNION ALL

    -- 13. DOCUMENTS
    SELECT 
        'DOCUMENT'::VARCHAR(50) AS entity_type,
        d.id AS entity_id,
        d.user_id AS user_id,
        d.dependent_id AS dependent_id,
        d.title::VARCHAR(255) AS title,
        (d.category || ' • ' || d.original_filename)::VARCHAR(255) AS subtitle,
        (coalesce(d.title, '') || ' ' || coalesce(d.original_filename, '') || ' ' || coalesce(substring(d.extracted_text, 1, 5000), ''))::TEXT AS content_text,
        d.category::VARCHAR(100) AS category_or_type,
        d.ingestion_status::VARCHAR(50) AS status,
        NULL::NUMERIC(14,2) AS amount,
        NULL::VARCHAR(3) AS currency,
        d.issue_date AS event_date,
        d.created_at AS created_at,
        d.is_deleted AS is_deleted,
        (setweight(to_tsvector('english', coalesce(d.title, '') || ' ' || coalesce(d.original_filename, '')), 'A') || 
         setweight(to_tsvector('english', coalesce(d.category, '') || ' ' || coalesce(d.document_type, '')), 'B') || 
         setweight(to_tsvector('english', coalesce(substring(d.extracted_text, 1, 5000), '')), 'C')) AS tsv_content
    FROM documents d

    UNION ALL

    -- 14. DEPENDENTS
    SELECT 
        'DEPENDENT'::VARCHAR(50) AS entity_type,
        dep.id AS entity_id,
        dep.user_id AS user_id,
        dep.id AS dependent_id,
        dep.full_name::VARCHAR(255) AS title,
        (dep.relationship || CASE WHEN dep.emergency_phone IS NOT NULL THEN ' • ' || dep.emergency_phone ELSE '' END)::VARCHAR(255) AS subtitle,
        (coalesce(dep.full_name, '') || ' ' || coalesce(dep.relationship, '') || ' ' || coalesce(dep.emergency_phone, ''))::TEXT AS content_text,
        dep.relationship::VARCHAR(100) AS category_or_type,
        'ACTIVE'::VARCHAR(50) AS status,
        NULL::NUMERIC(14,2) AS amount,
        NULL::VARCHAR(3) AS currency,
        dep.date_of_birth AS event_date,
        dep.created_at AS created_at,
        dep.is_deleted AS is_deleted,
        (setweight(to_tsvector('english', coalesce(dep.full_name, '')), 'A') || 
         setweight(to_tsvector('english', coalesce(dep.relationship, '')), 'B')) AS tsv_content
    FROM dependents dep

    UNION ALL

    -- 15. REMINDERS
    SELECT 
        'REMINDER'::VARCHAR(50) AS entity_type,
        rem.id AS entity_id,
        rem.user_id AS user_id,
        NULL::UUID AS dependent_id,
        rem.title::VARCHAR(255) AS title,
        (rem.reminder_type || ' • Due: ' || rem.due_at::text)::VARCHAR(255) AS subtitle,
        (coalesce(rem.title, '') || ' ' || coalesce(rem.description, '') || ' ' || coalesce(rem.reminder_type, ''))::TEXT AS content_text,
        rem.reminder_type::VARCHAR(100) AS category_or_type,
        rem.status::VARCHAR(50) AS status,
        NULL::NUMERIC(14,2) AS amount,
        NULL::VARCHAR(3) AS currency,
        rem.due_at::DATE AS event_date,
        rem.created_at AS created_at,
        rem.is_deleted AS is_deleted,
        (setweight(to_tsvector('english', coalesce(rem.title, '')), 'A') || 
         setweight(to_tsvector('english', coalesce(rem.reminder_type, '')), 'B') || 
         setweight(to_tsvector('english', coalesce(rem.description, '')), 'C')) AS tsv_content
    FROM reminders rem;
