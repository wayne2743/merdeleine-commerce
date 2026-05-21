-- Add recipe_quantity to product
ALTER TABLE product
    ADD COLUMN recipe_quantity integer NOT NULL DEFAULT 1
        CONSTRAINT product_recipe_quantity_check CHECK (recipe_quantity >= 1);

-- Create ingredient_group table
CREATE TABLE ingredient_group (
    id          uuid         PRIMARY KEY,
    product_id  uuid         NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    name        varchar(100) NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_ingredient_group_product_id ON ingredient_group(product_id);

-- Add ingredient_group_id FK to product_ingredient
ALTER TABLE product_ingredient
    ADD COLUMN ingredient_group_id uuid REFERENCES ingredient_group(id) ON DELETE SET NULL;

-- Replace unique constraint to include ingredient_group_id.
-- Use a unique index with COALESCE so that NULL ingredient_group_id is also treated as unique per (product, ingredient).
ALTER TABLE product_ingredient DROP CONSTRAINT IF EXISTS uq_product_ingredient;

CREATE UNIQUE INDEX uq_product_ingredient
    ON product_ingredient(product_id, ingredient_id, COALESCE(ingredient_group_id, '00000000-0000-0000-0000-000000000000'::uuid));
