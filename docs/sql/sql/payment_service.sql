CREATE TABLE public.payment (
                                id uuid NOT NULL,
                                order_id uuid NOT NULL,
                                provider varchar(30) NOT NULL,
                                status varchar(20) NOT NULL,
                                amount_cents int4 NOT NULL,
                                currency varchar(10) NOT NULL,
                                provider_payment_id varchar(100) NULL,
                                provider_capture_id varchar(100) NULL,
                                approve_url varchar(500) NULL,
                                bank_last_five varchar(5) NULL,
                                transfer_at timestamptz NULL,
                                created_at timestamptz DEFAULT now() NOT NULL,
                                updated_at timestamptz DEFAULT now() NOT NULL,
                                expire_at timestamptz NULL,
                                expired_at timestamptz NULL,
                                CONSTRAINT payment_amount_cents_check CHECK ((amount_cents >= 0)),
                                CONSTRAINT payment_pkey PRIMARY KEY (id),
                                CONSTRAINT payment_provider_check CHECK (((provider)::text = ANY ((ARRAY['BankTransfer'::character varying, 'NewebPay'::character varying, 'ECpay'::character varying, 'PayPal'::character varying, 'LinePay'::character varying])::text[]))),
	CONSTRAINT payment_status_check CHECK (((status)::text = ANY ((ARRAY['INIT'::character varying, 'SUCCEEDED'::character varying, 'PENDING'::character varying, 'AUTHORIZED'::character varying, 'PAID'::character varying, 'FAILED'::character varying, 'EXPIRED'::character varying, 'CANCELLED'::character varying, 'PARTIALLY_REFUNDED'::character varying, 'REFUNDED'::character varying])::text[])))
);
CREATE INDEX idx_payment_order_id ON public.payment USING btree (order_id);
CREATE INDEX idx_payment_status_created_at ON public.payment USING btree (status, created_at);
CREATE INDEX idx_payment_status_expire_at ON public.payment USING btree (status, expire_at) WHERE (expire_at IS NOT NULL);


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
