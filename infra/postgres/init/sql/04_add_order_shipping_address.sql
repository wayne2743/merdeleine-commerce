-- Add shipping address field to orders table
ALTER TABLE orders
ADD COLUMN IF NOT EXISTS shipping_address TEXT;

COMMENT ON COLUMN orders.shipping_address IS '訂單收件地址';

