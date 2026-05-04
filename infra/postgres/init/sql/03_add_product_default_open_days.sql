ALTER TABLE product
    ADD COLUMN IF NOT EXISTS default_open_days INTEGER;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'product_default_open_days_check'
    ) THEN
        ALTER TABLE product
            ADD CONSTRAINT product_default_open_days_check
                CHECK (default_open_days IS NULL OR default_open_days >= 0);
    END IF;
END
$$;

