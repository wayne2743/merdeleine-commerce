-- Migration: Add nutrition fields to product table
-- ingredients: 商品成分
-- allergens:   商品過敏原
-- calories:    卡路里 (kcal, nullable integer)

ALTER TABLE public.product
    ADD COLUMN IF NOT EXISTS ingredients  TEXT,
    ADD COLUMN IF NOT EXISTS allergens    TEXT,
    ADD COLUMN IF NOT EXISTS calories     INTEGER;

COMMENT ON COLUMN public.product.ingredients IS '商品成分說明';
COMMENT ON COLUMN public.product.allergens   IS '商品過敏原說明';
COMMENT ON COLUMN public.product.calories    IS '每份卡路里 (kcal)';

