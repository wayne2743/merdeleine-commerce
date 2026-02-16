CREATE TABLE product (
                         id UUID PRIMARY KEY,
                         name VARCHAR(255) NOT NULL,
                         description TEXT,
                         status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE')),
                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);



CREATE TABLE product_variant (
                                 id UUID PRIMARY KEY,
                                 product_id UUID NOT NULL,
                                 sku VARCHAR(100) NOT NULL,
                                 variant_name VARCHAR(100),
                                 price_cents INTEGER NOT NULL CHECK (price_cents >= 0),
                                 currency VARCHAR(10) NOT NULL,
                                 is_active BOOLEAN NOT NULL DEFAULT true,

                                 CONSTRAINT fk_product_variant_product
                                     FOREIGN KEY (product_id) REFERENCES product(id)
);


CREATE TABLE sell_window (
                             id uuid NOT NULL,
                             "name" varchar(100) NOT NULL,
                             start_at timestamptz NOT NULL,
                             end_at timestamptz NOT NULL,
                             timezone varchar(50) NOT NULL,
                             status varchar(20) NOT NULL,
                             closed_at timestamptz NULL,
                             "version" int8 NOT NULL,
                             CONSTRAINT sell_window_pkey PRIMARY KEY (id)
);


CREATE TABLE product_sell_window (
                                     id UUID PRIMARY KEY,
                                     product_id UUID NOT NULL,
                                     sell_window_id UUID NOT NULL,
                                     min_total_qty INTEGER NOT NULL CHECK (min_total_qty >= 0),
                                     max_total_qty INTEGER,
                                     lead_days INTEGER,
                                     ship_days INTEGER,
                                     is_closed BOOLEAN NOT NULL DEFAULT true,
                                     CONSTRAINT fk_psw_product
                                         FOREIGN KEY (product_id) REFERENCES product(id),
                                     CONSTRAINT fk_psw_sell_window
                                         FOREIGN KEY (sell_window_id) REFERENCES sell_window(id),
                                     CONSTRAINT uq_product_sell_window
                                         UNIQUE (product_id, sell_window_id)
);


CREATE TABLE public.outbox_event (
                                     id uuid NOT NULL,
                                     aggregate_type varchar(50) NOT NULL,
                                     aggregate_id uuid NOT NULL,
                                     event_type varchar(100) NOT NULL,
                                     idempotency_key varchar(200) NOT NULL,
                                     payload jsonb NOT NULL,
                                     status varchar(20) NOT NULL,
                                     created_at timestamptz DEFAULT now() NOT NULL,
                                     sent_at timestamptz NULL,
                                     CONSTRAINT ck_outbox_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SENT'::character varying, 'FAILED'::character varying])::text[]))),
	                                 CONSTRAINT outbox_event_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_outbox_status_created_at ON public.outbox_event USING btree (status, created_at);
CREATE UNIQUE INDEX uk_outbox_idempotency_key ON public.outbox_event USING btree (idempotency_key);
