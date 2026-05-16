-- Create order_delivery table linked one-to-one with orders
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

CREATE TABLE IF NOT EXISTS order_delivery (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    delivery_method VARCHAR(40) NOT NULL CHECK (
        delivery_method IN ('STORE_PICKUP', 'CONVENIENCE_STORE_PICKUP', 'HOME_DELIVERY')
    ),
    pickup_location_id UUID,
    pickup_location_name VARCHAR(255),
    pickup_location_address TEXT,
    pickup_time TIMESTAMPTZ,
    convenience_store_code VARCHAR(50),
    convenience_store_name VARCHAR(255),
    convenience_store_address TEXT,
    home_delivery_address TEXT,
    CONSTRAINT fk_order_delivery_order
        FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_delivery_pickup_location
        FOREIGN KEY (pickup_location_id) REFERENCES store_pickup_location(id)
);

CREATE INDEX IF NOT EXISTS idx_order_delivery_method
    ON order_delivery (delivery_method);

