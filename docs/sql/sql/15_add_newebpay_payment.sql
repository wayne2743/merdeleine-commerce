-- Add NewebPay payment/refund support to an existing payment_db.
-- Run this file against payment_db before deploying the updated payment-service.

UPDATE payment SET provider = 'NewebPay' WHERE provider = 'Newebpay';

ALTER TABLE payment
    ADD COLUMN IF NOT EXISTS provider_capture_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS approve_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS expire_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS expired_at TIMESTAMPTZ;

ALTER TABLE payment DROP CONSTRAINT IF EXISTS payment_provider_check;
ALTER TABLE payment ADD CONSTRAINT payment_provider_check CHECK (
    provider IN ('BankTransfer', 'NewebPay', 'ECpay', 'PayPal', 'LinePay')
);

ALTER TABLE payment DROP CONSTRAINT IF EXISTS payment_status_check;
ALTER TABLE payment ADD CONSTRAINT payment_status_check CHECK (
    status IN ('INIT', 'SUCCEEDED', 'PENDING', 'AUTHORIZED', 'PAID',
               'FAILED', 'EXPIRED', 'CANCELLED', 'PARTIALLY_REFUNDED', 'REFUNDED')
);

CREATE INDEX IF NOT EXISTS idx_payment_status_expire_at
    ON payment(status, expire_at) WHERE expire_at IS NOT NULL;

CREATE TABLE IF NOT EXISTS payment_refund (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'UNKNOWN')),
    provider_trade_no VARCHAR(100),
    provider_code VARCHAR(50),
    provider_message VARCHAR(500),
    raw_response JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_refund_payment FOREIGN KEY (payment_id) REFERENCES payment(id),
    CONSTRAINT uk_payment_refund_payment_idempotency UNIQUE (payment_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_payment_refund_payment_id ON payment_refund(payment_id);
