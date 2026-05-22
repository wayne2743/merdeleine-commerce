-- Remove product_id from ingredient_group. Relationship to product is now indirect via product_ingredient.
ALTER TABLE ingredient_group DROP CONSTRAINT IF EXISTS ingredient_group_product_id_fkey;
DROP INDEX IF EXISTS idx_ingredient_group_product_id;
ALTER TABLE ingredient_group DROP COLUMN IF EXISTS product_id;
