--
-- TOC entry 218 (class 1259 OID 41183)
-- Name: outbox_event; Type: TABLE; Schema: public; Owner: merdeleine
--

CREATE TABLE public.outbox_event (
                                     id uuid NOT NULL,
                                     aggregate_type character varying(50) NOT NULL,
                                     aggregate_id uuid NOT NULL,
                                     event_type character varying(100) NOT NULL,
                                     idempotency_key character varying(200) NOT NULL,
                                     payload jsonb NOT NULL,
                                     status character varying(20) NOT NULL,
                                     created_at timestamp with time zone DEFAULT now() NOT NULL,
                                     sent_at timestamp with time zone,
                                     CONSTRAINT ck_outbox_status CHECK (((status)::text = ANY (ARRAY[('PENDING'::character varying)::text, ('SENT'::character varying)::text, ('FAILED'::character varying)::text])))
);


ALTER TABLE public.outbox_event OWNER TO merdeleine;

--
-- TOC entry 215 (class 1259 OID 16391)
-- Name: product; Type: TABLE; Schema: public; Owner: merdeleine
--

CREATE TABLE public.product (
                                id uuid NOT NULL,
                                name character varying(255) NOT NULL,
                                description text,
                                status character varying(20) NOT NULL,
                                created_at timestamp with time zone DEFAULT now() NOT NULL,
                                updated_at timestamp with time zone DEFAULT now() NOT NULL,
                                CONSTRAINT product_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


ALTER TABLE public.product OWNER TO merdeleine;

--
-- TOC entry 217 (class 1259 OID 24799)
-- Name: product_sell_window; Type: TABLE; Schema: public; Owner: merdeleine
--

CREATE TABLE public.product_sell_window (
                                            id uuid NOT NULL,
                                            product_id uuid NOT NULL,
                                            sell_window_id uuid NOT NULL,
                                            min_total_qty integer NOT NULL,
                                            max_total_qty integer,
                                            lead_days integer,
                                            ship_days integer,
                                            is_closed boolean DEFAULT true NOT NULL,
                                            unit_price_cents integer DEFAULT 0 NOT NULL,
                                            currency character varying(10) DEFAULT 'TWD'::character varying NOT NULL,
                                            CONSTRAINT product_sell_window_min_total_qty_check CHECK ((min_total_qty >= 0))
);


ALTER TABLE public.product_sell_window OWNER TO merdeleine;

--
-- TOC entry 216 (class 1259 OID 16413)
-- Name: sell_window; Type: TABLE; Schema: public; Owner: merdeleine
--

CREATE TABLE public.sell_window (
                                    id uuid NOT NULL,
                                    name character varying(100) NOT NULL,
                                    start_at timestamp with time zone NOT NULL,
                                    end_at timestamp with time zone NOT NULL,
                                    timezone character varying(50) NOT NULL,
                                    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
                                    closed_at timestamp with time zone,
                                    version bigint DEFAULT 0 NOT NULL,
                                    payment_ttl_minutes integer DEFAULT 1440 NOT NULL,
                                    payment_opened_at timestamp with time zone,
                                    payment_close_at timestamp with time zone,
                                    CONSTRAINT chk_sell_window_payment_ttl_positive CHECK ((payment_ttl_minutes > 0)),
                                    CONSTRAINT chk_sell_window_payment_window_valid CHECK (((payment_opened_at IS NULL) OR (payment_close_at IS NULL) OR (payment_close_at >= payment_opened_at))),
                                    CONSTRAINT ck_sell_window_status CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'OPEN'::character varying, 'PAYMENT_OPEN'::character varying, 'PAYMENT_CLOSED'::character varying, 'CLOSED'::character varying])::text[])))
);


ALTER TABLE public.sell_window OWNER TO merdeleine;

--
-- TOC entry 3304 (class 2606 OID 41191)
-- Name: outbox_event outbox_event_pkey; Type: CONSTRAINT; Schema: public; Owner: merdeleine
--

ALTER TABLE ONLY public.outbox_event
    ADD CONSTRAINT outbox_event_pkey PRIMARY KEY (id);


--
-- TOC entry 3294 (class 2606 OID 16400)
-- Name: product product_pkey; Type: CONSTRAINT; Schema: public; Owner: merdeleine
--

ALTER TABLE ONLY public.product
    ADD CONSTRAINT product_pkey PRIMARY KEY (id);


--
-- TOC entry 3299 (class 2606 OID 24805)
-- Name: product_sell_window product_sell_window_pkey; Type: CONSTRAINT; Schema: public; Owner: merdeleine
--

ALTER TABLE ONLY public.product_sell_window
    ADD CONSTRAINT product_sell_window_pkey PRIMARY KEY (id);


--
-- TOC entry 3297 (class 2606 OID 16417)
-- Name: sell_window sell_window_pkey; Type: CONSTRAINT; Schema: public; Owner: merdeleine
--

ALTER TABLE ONLY public.sell_window
    ADD CONSTRAINT sell_window_pkey PRIMARY KEY (id);


--
-- TOC entry 3301 (class 2606 OID 24807)
-- Name: product_sell_window uq_product_sell_window; Type: CONSTRAINT; Schema: public; Owner: merdeleine
--

ALTER TABLE ONLY public.product_sell_window
    ADD CONSTRAINT uq_product_sell_window UNIQUE (product_id, sell_window_id);


--
-- TOC entry 3302 (class 1259 OID 41192)
-- Name: idx_outbox_status_created_at; Type: INDEX; Schema: public; Owner: merdeleine
--

CREATE INDEX idx_outbox_status_created_at ON public.outbox_event USING btree (status, created_at);


--
-- TOC entry 3295 (class 1259 OID 49363)
-- Name: idx_sell_window_payment_close_at; Type: INDEX; Schema: public; Owner: merdeleine
--

CREATE INDEX idx_sell_window_payment_close_at ON public.sell_window USING btree (payment_close_at) WHERE (payment_close_at IS NOT NULL);


--
-- TOC entry 3305 (class 1259 OID 41193)
-- Name: uk_outbox_idempotency_key; Type: INDEX; Schema: public; Owner: merdeleine
--

CREATE UNIQUE INDEX uk_outbox_idempotency_key ON public.outbox_event USING btree (idempotency_key);


--
-- TOC entry 3306 (class 2606 OID 24808)
-- Name: product_sell_window fk_psw_product; Type: FK CONSTRAINT; Schema: public; Owner: merdeleine
--

ALTER TABLE ONLY public.product_sell_window
    ADD CONSTRAINT fk_psw_product FOREIGN KEY (product_id) REFERENCES public.product(id);


--
-- TOC entry 3307 (class 2606 OID 24813)
-- Name: product_sell_window fk_psw_sell_window; Type: FK CONSTRAINT; Schema: public; Owner: merdeleine
--

ALTER TABLE ONLY public.product_sell_window
    ADD CONSTRAINT fk_psw_sell_window FOREIGN KEY (sell_window_id) REFERENCES public.sell_window(id);