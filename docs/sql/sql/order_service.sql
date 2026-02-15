CREATE TABLE orders (
                        id UUID PRIMARY KEY,
                        order_no VARCHAR(50) NOT NULL UNIQUE,
                        customer_id UUID NOT NULL,
                        sell_window_id UUID,

    -- Updated statuses:
    -- RESERVED: 預約名額(未付款)
    -- PAYMENT_REQUESTED: 已開放付款(可重試 payment.failed)
    -- PAID: 已付款
    -- EXPIRED: 付款逾期(釋放名額)
    -- CANCELLED: 主動取消(釋放名額)
    -- REFUNDED: 已退款
                        status VARCHAR(30) NOT NULL CHECK (
                            status IN ('RESERVED', 'PAYMENT_REQUESTED', 'PAID', 'EXPIRED', 'CANCELLED', 'REFUNDED')
                            ),

                        total_amount_cents INTEGER NOT NULL CHECK (total_amount_cents >= 0),
                        currency VARCHAR(10) NOT NULL,

                        contact_name VARCHAR(100),
                        contact_phone VARCHAR(30),
                        contact_email VARCHAR(255),
                        shipping_address TEXT,

    -- New fields for this flow
                        payment_due_at TIMESTAMPTZ,
                        payment_failed_count INTEGER NOT NULL DEFAULT 0,
                        last_payment_error TEXT,

                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- (optional but useful) 查詢付款期限/狀態的索引
CREATE INDEX idx_orders_status_due_at
    ON orders (status, payment_due_at);

CREATE INDEX idx_orders_sell_window_status
    ON orders (sell_window_id, status);


CREATE TABLE order_item (
                            id UUID PRIMARY KEY,
                            order_id UUID NOT NULL,

                            product_id UUID NOT NULL,

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


CREATE TABLE sell_window_quota (
                                   id UUID PRIMARY KEY,
                                   sell_window_id UUID NOT NULL,
                                   product_id UUID NOT NULL,

                                   min_qty INT NOT NULL DEFAULT 0,
                                   max_qty INT NOT NULL,

                                   sold_qty INT NOT NULL DEFAULT 0,

                                   status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                                       CHECK (status IN ('OPEN', 'CLOSED')),

                                   updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                   CONSTRAINT uq_quota UNIQUE (sell_window_id, product_id),
                                   CONSTRAINT ck_qty_nonneg CHECK (sold_qty >= 0),
                                   CONSTRAINT ck_max_positive CHECK (max_qty > 0)
);

CREATE INDEX idx_quota_lookup
    ON sell_window_quota (sell_window_id, product_id);