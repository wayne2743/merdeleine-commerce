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