-- Remove price/stock fields from ingredient (moving to stock table)
ALTER TABLE ingredient DROP CONSTRAINT IF EXISTS ingredient_date_range_check;
ALTER TABLE ingredient
    DROP COLUMN IF EXISTS unit_price_cents,
    DROP COLUMN IF EXISTS stocked_at,
    DROP COLUMN IF EXISTS expires_at,
    DROP COLUMN IF EXISTS stock_quantity;

-- Create stock table (1 ingredient : many stocks)
CREATE TABLE stock (
    id               uuid PRIMARY KEY,
    ingredient_id    uuid NOT NULL REFERENCES ingredient(id) ON DELETE CASCADE,
    unit_price_cents integer NOT NULL CHECK (unit_price_cents >= 0),
    stocked_at       date,
    expires_at       date,
    stock_quantity   numeric(14,3) NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT stock_date_range_check
        CHECK (expires_at IS NULL OR stocked_at IS NULL OR expires_at >= stocked_at)
);

CREATE INDEX idx_stock_ingredient_id ON stock(ingredient_id);
