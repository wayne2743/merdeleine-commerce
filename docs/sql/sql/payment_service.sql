CREATE TABLE payment (
                         id UUID PRIMARY KEY,
                         order_id UUID NOT NULL,

                         provider VARCHAR(30) NOT NULL CHECK (
                             provider IN ('ECpay', 'Newebpay', 'LinePay')
                             ),

                         status VARCHAR(20) NOT NULL CHECK (
                             status IN ('INIT', 'SUCCEEDED', 'FAILED', 'EXPIRED', 'REFUNDED')
                             ),

                         amount_cents INTEGER NOT NULL CHECK (amount_cents >= 0),
                         currency VARCHAR(10) NOT NULL,

                         provider_payment_id VARCHAR(100),

                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_order_id
    ON payment (order_id);

CREATE INDEX idx_payment_status_created_at
    ON payment (status, created_at);


CREATE TABLE payment_txn (
                             id UUID PRIMARY KEY,
                             payment_id UUID NOT NULL,

                             action VARCHAR(20) NOT NULL CHECK (
                                 action IN ('AUTHORIZE', 'CAPTURE', 'REFUND')
),

  result VARCHAR(10) NOT NULL CHECK (result IN ('OK', 'NG')),
  raw_response JSONB,

  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_payment_txn_payment
    FOREIGN KEY (payment_id) REFERENCES payment(id)
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
