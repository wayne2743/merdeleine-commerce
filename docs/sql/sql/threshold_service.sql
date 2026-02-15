CREATE TABLE batch_counter (
                               id UUID PRIMARY KEY,

                               sell_window_id UUID NOT NULL,
                               product_id UUID NOT NULL,

                               reserved_qty INTEGER NOT NULL DEFAULT 0 CHECK (reserved_qty >= 0),
                               paid_qty INTEGER NOT NULL DEFAULT 0 CHECK (paid_qty >= 0),

                               threshold_qty INTEGER NOT NULL CHECK (threshold_qty >= 0),

                               status VARCHAR(20) NOT NULL CHECK (
                                   status IN ('OPEN', 'REACHED', 'LOCKED', 'FINALIZED')
                                   ),

                               reached_at TIMESTAMPTZ,
                               reached_event_id UUID,

    -- finalize snapshot (recommended)
                               final_paid_qty INTEGER NULL CHECK (final_paid_qty IS NULL OR final_paid_qty >= 0),
                               finalized_at TIMESTAMPTZ NULL,

                               updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                               CONSTRAINT uq_counter UNIQUE (sell_window_id, product_id)
);

CREATE INDEX idx_counter_lookup
    ON batch_counter (sell_window_id, product_id);

CREATE INDEX idx_counter_status_updated_at
    ON batch_counter (status, updated_at);


CREATE TABLE counter_event_log (
                                   id UUID PRIMARY KEY,
                                   counter_id UUID NOT NULL,

                                   source_event_type VARCHAR(100) NOT NULL,
                                   source_event_id UUID NOT NULL,

                                   delta_qty INTEGER NOT NULL,
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                   CONSTRAINT fk_counter_event
                                       FOREIGN KEY (counter_id) REFERENCES batch_counter(id)
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