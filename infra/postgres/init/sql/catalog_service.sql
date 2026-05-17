CREATE TABLE product (
                         id UUID PRIMARY KEY,
                         name VARCHAR(100) NOT NULL,
                         description VARCHAR(500),
                         status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE')),
                         unit_price_cents INTEGER NOT NULL DEFAULT 0,
                         currency VARCHAR(10) NOT NULL DEFAULT 'TWD',
                         default_min_qty INTEGER NOT NULL DEFAULT 1 CHECK (default_min_qty >= 1),
                         default_max_qty INTEGER,
                         default_lead_days INTEGER,
                         default_ship_days INTEGER,
                         default_open_days INTEGER,
                         ingredients TEXT,
                         allergens TEXT,
                         calories INTEGER,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         CONSTRAINT product_default_qty_range_check
                             CHECK (default_max_qty IS NULL OR default_max_qty >= default_min_qty),
                         CONSTRAINT product_default_lead_days_check
                             CHECK (default_lead_days IS NULL OR default_lead_days >= 0),
                         CONSTRAINT product_default_ship_days_check
                             CHECK (default_ship_days IS NULL OR default_ship_days >= 0),
                         CONSTRAINT product_default_open_days_check
                             CHECK (default_open_days IS NULL OR default_open_days >= 0),
                         CONSTRAINT product_calories_check
                             CHECK (calories IS NULL OR calories >= 0)
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


CREATE TABLE ingredient (
                           id UUID PRIMARY KEY,
                           name VARCHAR(100) NOT NULL,
                           unit_price_cents INTEGER NOT NULL CHECK (unit_price_cents >= 0),
                           brand VARCHAR(100),
                           origin VARCHAR(100),
                           government_registration_info VARCHAR(1000),
                           attribute VARCHAR(20) NOT NULL CHECK (attribute IN ('DRY', 'WET')),
                           stocked_at DATE,
                           expires_at DATE,
                           stock_quantity NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
                           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           CONSTRAINT ingredient_date_range_check
                               CHECK (expires_at IS NULL OR stocked_at IS NULL OR expires_at >= stocked_at)
);

CREATE TABLE product_ingredient (
                                  id UUID PRIMARY KEY,
                                  product_id UUID NOT NULL,
                                  ingredient_id UUID NOT NULL,
                                  required_amount NUMERIC(12,3) NOT NULL CHECK (required_amount > 0),
                                  unit VARCHAR(30) NOT NULL,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  CONSTRAINT fk_product_ingredient_product
                                      FOREIGN KEY (product_id) REFERENCES product(id),
                                  CONSTRAINT fk_product_ingredient_ingredient
                                      FOREIGN KEY (ingredient_id) REFERENCES ingredient(id),
                                  CONSTRAINT uq_product_ingredient
                                      UNIQUE (product_id, ingredient_id),
                                  CONSTRAINT product_ingredient_unit_check
                                      CHECK (unit IN ('G', 'KG', 'ML', 'L', 'PCS'))
);

CREATE INDEX idx_product_ingredient_product_id ON product_ingredient(product_id);
CREATE INDEX idx_product_ingredient_ingredient_id ON product_ingredient(ingredient_id);


CREATE TABLE sell_window (
                                    id uuid NOT NULL,
                                    "name" varchar(100) NOT NULL,
                                    start_at timestamptz NOT NULL,
                                    end_at timestamptz NOT NULL,
                                    timezone varchar(50) NOT NULL,
                                    status varchar(20) NOT NULL,
                                    closed_at timestamptz NULL,
                                    predicted_payment_date timestamptz NULL,
                                    predicted_prod_date timestamptz NULL,
                                    predicted_ship_date timestamptz NULL,
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
