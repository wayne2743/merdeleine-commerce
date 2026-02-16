CREATE TABLE work_order (
                                   id uuid NOT NULL,
                                   batch_id uuid NOT NULL,
                                   product_qty int4 NOT NULL,
                                   status varchar(20) NOT NULL,
                                   start_at timestamptz NULL,
                                   end_at timestamptz NULL,
                                   "operator" varchar(100) NULL,
                                   CONSTRAINT work_order_pkey PRIMARY KEY (id),
                                   CONSTRAINT work_order_status_check CHECK (((status)::text = ANY ((ARRAY['READY'::character varying, 'IN_PROGRESS'::character varying, 'DONE'::character varying, 'FAILED'::character varying])::text[])))
);


CREATE TABLE work_step (
                           id UUID PRIMARY KEY,
                           work_order_id UUID NOT NULL,

                           step_name VARCHAR(100) NOT NULL,
                           status VARCHAR(20) NOT NULL CHECK (
                               status IN ('TODO', 'DOING', 'DONE')
                               ),

                           notes TEXT,

                           CONSTRAINT fk_work_step
                               FOREIGN KEY (work_order_id) REFERENCES work_order(id)
);
