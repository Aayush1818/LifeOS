-- ============================================================================
-- V6: Travel & Trip Itinerary Management Enhancements
-- ============================================================================

-- 1. Enhance foundational trips table
ALTER TABLE trips
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    ADD COLUMN IF NOT EXISTS cover_image_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS metadata JSONB NOT NULL DEFAULT '{}'::jsonb;

-- Widen monetary columns to NUMERIC(14,2)
ALTER TABLE trips 
    ALTER COLUMN total_budget TYPE NUMERIC(14,2),
    ALTER COLUMN actual_spend TYPE NUMERIC(14,2);

-- 2. Create trip_travelers table
CREATE TABLE IF NOT EXISTS trip_travelers (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id           UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    dependent_id      UUID REFERENCES dependents(id) ON DELETE SET NULL,
    traveler_name     VARCHAR(150) NOT NULL,
    is_primary_user   BOOLEAN NOT NULL DEFAULT FALSE,
    notes             VARCHAR(255),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_trip_dependent UNIQUE (trip_id, dependent_id)
);

CREATE INDEX IF NOT EXISTS idx_trip_travelers_trip ON trip_travelers(trip_id);
CREATE INDEX IF NOT EXISTS idx_trip_travelers_dependent ON trip_travelers(dependent_id) WHERE dependent_id IS NOT NULL;

-- 3. Create unified, extensible itinerary_items table
CREATE TABLE IF NOT EXISTS itinerary_items (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id                 UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    user_id                 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_type               VARCHAR(50) NOT NULL, -- FLIGHT, TRAIN, BUS, LODGING, ACTIVITY, RESTAURANT, RENTAL_CAR, TRANSFER, CUSTOM
    custom_type_name        VARCHAR(100),         -- Extensible user-defined type descriptor
    title                   VARCHAR(200) NOT NULL,
    provider                VARCHAR(150),         -- Airline, Hotel, Tour operator, Railway
    booking_reference       VARCHAR(100),         -- PNR, confirmation number
    confirmation_details    TEXT,
    start_time              TIMESTAMP WITH TIME ZONE NOT NULL,
    start_time_zone         VARCHAR(50) NOT NULL DEFAULT 'UTC', -- IANA zone id: America/New_York, Asia/Kolkata, etc.
    start_location          VARCHAR(255),
    end_time                TIMESTAMP WITH TIME ZONE,
    end_time_zone           VARCHAR(50) NOT NULL DEFAULT 'UTC', -- IANA zone id: Europe/London, Europe/Paris, etc.
    end_location            VARCHAR(255),
    status                  VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED', -- CONFIRMED, TENTATIVE, WAITLISTED, CANCELLED, COMPLETED
    cost                    NUMERIC(14,2),
    currency                VARCHAR(3) NOT NULL DEFAULT 'USD',
    exchange_rate_to_base   NUMERIC(12,6),        -- Optional user-supplied exchange rate to trip base currency
    reminder_offset_minutes INT,                  -- Offset before start_time for reminder (e.g., 1440 for 24h)
    notes                   TEXT,
    metadata                JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Multi-tenant and temporal query indexes
CREATE INDEX IF NOT EXISTS idx_itinerary_trip_start 
    ON itinerary_items(trip_id, start_time) 
    WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_itinerary_user_start 
    ON itinerary_items(user_id, start_time) 
    WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_itinerary_user_type 
    ON itinerary_items(user_id, item_type) 
    WHERE NOT is_deleted;

CREATE INDEX IF NOT EXISTS idx_itinerary_booking_ref 
    ON itinerary_items(user_id, booking_reference) 
    WHERE booking_reference IS NOT NULL AND NOT is_deleted;

-- 5. Enhance trip_expenses table for itemized expense tracking
ALTER TABLE trip_expenses
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    ADD COLUMN IF NOT EXISTS exchange_rate_to_base NUMERIC(12,6),
    ADD COLUMN IF NOT EXISTS itinerary_item_id UUID REFERENCES itinerary_items(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS document_id UUID REFERENCES documents(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ALTER COLUMN amount TYPE NUMERIC(14,2);

CREATE INDEX IF NOT EXISTS idx_trip_expenses_itinerary 
    ON trip_expenses(itinerary_item_id) 
    WHERE itinerary_item_id IS NOT NULL AND NOT is_deleted;
