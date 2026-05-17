-- Migration: Restrict product_ingredient.unit values to IngredientUnit enum domain

ALTER TABLE public.product_ingredient
    DROP CONSTRAINT IF EXISTS product_ingredient_unit_check;

ALTER TABLE public.product_ingredient
    ADD CONSTRAINT product_ingredient_unit_check
        CHECK (unit IN ('G', 'KG', 'ML', 'L', 'PCS'));

