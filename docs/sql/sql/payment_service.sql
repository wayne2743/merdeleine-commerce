CREATE TABLE public.payment (
                                id uuid NOT NULL,
                                order_id uuid NOT NULL,
                                provider varchar(30) NOT NULL,
                                status varchar(20) NOT NULL,
                                amount_cents int4 NOT NULL,
                                currency varchar(10) NOT NULL,
                                provider_payment_id varchar(100) NULL,
                                created_at timestamptz DEFAULT now() NOT NULL,
                                updated_at timestamptz DEFAULT now() NOT NULL,
                                expire_at timestamptz NULL,
                                expired_at timestamptz NULL,
                                CONSTRAINT payment_amount_cents_check CHECK ((amount_cents >= 0)),
                                CONSTRAINT payment_pkey PRIMARY KEY (id),
                                CONSTRAINT payment_provider_check CHECK (((provider)::text = ANY ((ARRAY['ECpay'::character varying, 'Newebpay'::character varying, 'LinePay'::character varying])::text[]))),
	CONSTRAINT payment_status_check CHECK (((status)::text = ANY ((ARRAY['INIT'::character varying, 'SUCCEEDED'::character varying, 'FAILED'::character varying, 'EXPIRED'::character varying, 'REFUNDED'::character varying])::text[])))
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
