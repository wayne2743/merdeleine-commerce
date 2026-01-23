CREATE TABLE orders (
                        id UUID PRIMARY KEY,
                        order_no VARCHAR(50) NOT NULL UNIQUE,
                        customer_id UUID NOT NULL,
                        sell_window_id UUID,

                        status VARCHAR(30) NOT NULL CHECK (
                            status IN ('PENDING_PAYMENT', 'PAID', 'CANCELLED', 'REFUNDED')
                            ),

                        total_amount_cents INTEGER NOT NULL CHECK (total_amount_cents >= 0),
                        currency VARCHAR(10) NOT NULL,

                        contact_name VARCHAR(100),
                        contact_phone VARCHAR(30),
                        contact_email VARCHAR(255),
                        shipping_address TEXT,

                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


CREATE TABLE order_item (
                            id UUID PRIMARY KEY,
                            order_id UUID NOT NULL,

                            product_id UUID NOT NULL,
                            variant_id UUID NOT NULL,

                            quantity INTEGER NOT NULL CHECK (quantity > 0),
                            unit_price_cents INTEGER NOT NULL CHECK (unit_price_cents >= 0),
                            subtotal_cents INTEGER NOT NULL CHECK (subtotal_cents >= 0),

                            CONSTRAINT fk_order_item_order
                                FOREIGN KEY (order_id) REFERENCES orders(id)
);


CREATE TABLE outbox_event (
                              id UUID PRIMARY KEY,

                              aggregate_type VARCHAR(50) NOT NULL,
                              aggregate_id UUID NOT NULL,

                              event_type VARCHAR(100) NOT NULL,
                              payload JSONB NOT NULL,

                              status VARCHAR(20) NOT NULL CHECK (status IN ('NEW', 'SENT', 'FAILED')),

                              created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              sent_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_event_status
    ON outbox_event (status, created_at);
