CREATE TABLE notification_job (
                                  id UUID PRIMARY KEY,
                                  channel VARCHAR(20) NOT NULL CHECK (
                                      channel IN ('EMAIL', 'SMS', 'LINE', 'SLACK')
                                      ),
                                  recipient VARCHAR(255) NOT NULL,
                                  template_key VARCHAR(100) NOT NULL,

                                  payload JSONB NOT NULL,

                                  status VARCHAR(20) NOT NULL CHECK (
                                      status IN ('REQUESTED', 'SENT', 'FAILED')
                                      ),
                                  retry_count INTEGER NOT NULL DEFAULT 0,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  sent_at TIMESTAMPTZ
);
