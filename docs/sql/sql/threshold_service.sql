CREATE TABLE batch_counter (
                               id UUID PRIMARY KEY,

                               sell_window_id UUID NOT NULL,
                               product_id UUID NOT NULL,

                               paid_qty INTEGER NOT NULL DEFAULT 0 CHECK (paid_qty >= 0),
                               threshold_qty INTEGER NOT NULL CHECK (threshold_qty >= 0),

                               status VARCHAR(20) NOT NULL CHECK (
                                   status IN ('OPEN', 'REACHED', 'LOCKED')
                                   ),

                               reached_at TIMESTAMPTZ,
                               reached_event_id UUID,

                               updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                               CONSTRAINT uq_counter UNIQUE (sell_window_id, product_id)
);


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
