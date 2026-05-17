-- Migration: Add ingredient table and product-ingredient mapping table

CREATE TABLE IF NOT EXISTS public.ingredient (
    id uuid PRIMARY KEY,
    name varchar(100) NOT NULL,
    unit_price_cents integer NOT NULL CHECK (unit_price_cents >= 0),
    brand varchar(100),
    origin varchar(100),
    government_registration_info varchar(1000),
    attribute varchar(20) NOT NULL CHECK (attribute IN ('DRY', 'WET')),
    stocked_at date,
    expires_at date,
    stock_quantity numeric(14,3) NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ingredient_date_range_check CHECK (expires_at IS NULL OR stocked_at IS NULL OR expires_at >= stocked_at)
);

CREATE TABLE IF NOT EXISTS public.product_ingredient (
    id uuid PRIMARY KEY,
    product_id uuid NOT NULL REFERENCES public.product(id),
    ingredient_id uuid NOT NULL REFERENCES public.ingredient(id),
    required_amount numeric(12,3) NOT NULL CHECK (required_amount > 0),
    unit varchar(30) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_product_ingredient UNIQUE (product_id, ingredient_id),
    CONSTRAINT product_ingredient_unit_check CHECK (unit IN ('G', 'KG', 'ML', 'L', 'PCS'))
);

CREATE INDEX IF NOT EXISTS idx_product_ingredient_product_id ON public.product_ingredient(product_id);
CREATE INDEX IF NOT EXISTS idx_product_ingredient_ingredient_id ON public.product_ingredient(ingredient_id);

