-- Add master table for STORE_PICKUP locations and link order_delivery to selected location
CREATE TABLE IF NOT EXISTS store_pickup_location (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    contact_phone VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_store_pickup_location_active
    ON store_pickup_location (active, created_at);

ALTER TABLE order_delivery
    ADD COLUMN IF NOT EXISTS pickup_location_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_order_delivery_pickup_location'
    ) THEN
        ALTER TABLE order_delivery
            ADD CONSTRAINT fk_order_delivery_pickup_location
            FOREIGN KEY (pickup_location_id) REFERENCES store_pickup_location(id);
    END IF;
END $$;

