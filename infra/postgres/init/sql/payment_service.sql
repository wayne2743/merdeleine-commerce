CREATE TABLE payment (
                         id UUID PRIMARY KEY,
                         order_id UUID NOT NULL,

                         provider VARCHAR(30) NOT NULL CHECK (
                             provider IN ('BankTransfer', 'NewebPay', 'ECpay', 'PayPal', 'LinePay')
                             ),

                         status VARCHAR(20) NOT NULL CHECK (
                             status IN ('INIT', 'SUCCEEDED', 'PENDING', 'AUTHORIZED', 'PAID',
                                        'FAILED', 'EXPIRED', 'CANCELLED', 'PARTIALLY_REFUNDED', 'REFUNDED')
                             ),

                         amount_cents INTEGER NOT NULL CHECK (amount_cents >= 0),
                         currency VARCHAR(10) NOT NULL,

                         provider_payment_id VARCHAR(100),
                         provider_capture_id VARCHAR(100),
                         approve_url VARCHAR(500),
                         bank_last_five VARCHAR(5),
                         transfer_at TIMESTAMPTZ,
                         expire_at TIMESTAMPTZ,
                         expired_at TIMESTAMPTZ,

                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_order_id ON payment(order_id);
CREATE INDEX idx_payment_status_created_at ON payment(status, created_at);
CREATE INDEX idx_payment_status_expire_at ON payment(status, expire_at) WHERE expire_at IS NOT NULL;


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

CREATE TABLE payment_refund (
                                id UUID PRIMARY KEY,
                                payment_id UUID NOT NULL,
                                idempotency_key VARCHAR(100) NOT NULL,
                                amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
                                status VARCHAR(20) NOT NULL CHECK (
                                    status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'UNKNOWN')
                                    ),
                                provider_trade_no VARCHAR(100),
                                provider_code VARCHAR(50),
                                provider_message VARCHAR(500),
                                raw_response JSONB,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                CONSTRAINT fk_payment_refund_payment
                                    FOREIGN KEY (payment_id) REFERENCES payment(id),
                                CONSTRAINT uk_payment_refund_payment_idempotency
                                    UNIQUE (payment_id, idempotency_key)
);

CREATE INDEX idx_payment_refund_payment_id ON payment_refund(payment_id);


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
