-- Add calories_per_100g and allergens to ingredient table
ALTER TABLE ingredient
    ADD COLUMN calories_per_100g integer,
    ADD COLUMN allergens        varchar(500),
    ADD CONSTRAINT ingredient_calories_per_100g_check
        CHECK (calories_per_100g IS NULL OR calories_per_100g >= 0);

-- Remove ingredients, allergens, calories from product table
ALTER TABLE product
    DROP COLUMN IF EXISTS ingredients,
    DROP COLUMN IF EXISTS allergens,
    DROP COLUMN IF EXISTS calories;
