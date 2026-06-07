-- LINE Account Link: nonce table + tighten line_user uniqueness
CREATE TABLE line_account_link_nonce (
    id            uuid         PRIMARY KEY,
    nonce         varchar(255) NOT NULL UNIQUE,
    member_id     uuid         NOT NULL,
    line_user_id  varchar(100) NOT NULL,
    link_token    varchar(255) NOT NULL,
    status        varchar(30)  NOT NULL CHECK (status IN ('PENDING', 'USED', 'EXPIRED')),
    expired_at    timestamptz  NOT NULL,
    used_at       timestamptz  NULL,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL
);

CREATE INDEX idx_line_account_link_nonce_member_id
    ON line_account_link_nonce(member_id);

CREATE INDEX idx_line_account_link_nonce_line_user_id
    ON line_account_link_nonce(line_user_id);

-- Ensure a LINE userId maps to at most one row, and a member binds at most one LINE account.
CREATE UNIQUE INDEX IF NOT EXISTS uk_line_user_user_id
    ON line_user(user_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_line_user_member_id
    ON line_user(member_id)
    WHERE member_id IS NOT NULL;
