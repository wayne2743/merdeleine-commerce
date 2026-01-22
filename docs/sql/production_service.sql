CREATE TABLE work_order (
                            id UUID PRIMARY KEY,
                            batch_id UUID NOT NULL,

                            status VARCHAR(20) NOT NULL CHECK (
                                status IN ('READY', 'IN_PROGRESS', 'DONE', 'FAILED')
                                ),

                            start_at TIMESTAMPTZ,
                            end_at TIMESTAMPTZ,

                            operator VARCHAR(100)
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
