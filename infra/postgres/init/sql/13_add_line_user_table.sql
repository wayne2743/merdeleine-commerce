-- line_user table for notification_db
CREATE TABLE line_user (
    id           UUID         PRIMARY KEY,
    user_id      VARCHAR(100) NOT NULL UNIQUE,   -- LINE's userId (e.g. "Uxxx...")
    member_id    UUID,                           -- nullable; maps to customerId in the order system
    display_name VARCHAR(200),
    followed     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_line_user_member_id ON line_user(member_id);
