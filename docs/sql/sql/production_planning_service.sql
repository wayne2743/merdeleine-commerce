CREATE TABLE batch (
                       id UUID PRIMARY KEY,

                       sell_window_id UUID NOT NULL,
                       product_id UUID NOT NULL,

                       target_qty INTEGER NOT NULL CHECK (target_qty > 0),

                       status VARCHAR(20) NOT NULL CHECK (
                           status IN ('CREATED', 'CONFIRMED', 'CANCELLED')
                           ),

                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       confirmed_at TIMESTAMPTZ
);


CREATE TABLE batch_order_link (
                                  id UUID PRIMARY KEY,
                                  batch_id UUID NOT NULL,
                                  order_id UUID NOT NULL,
                                  quantity INTEGER NOT NULL CHECK (quantity > 0),

                                  CONSTRAINT fk_batch_order
                                      FOREIGN KEY (batch_id) REFERENCES batch(id)
);


CREATE TABLE batch_schedule (
                                id UUID PRIMARY KEY,
                                batch_id UUID NOT NULL,

                                planned_production_date TIMESTAMPTZ,
                                planned_ship_date TIMESTAMPTZ,

                                notes TEXT,
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                CONSTRAINT fk_batch_schedule
                                    FOREIGN KEY (batch_id) REFERENCES batch(id)
);
