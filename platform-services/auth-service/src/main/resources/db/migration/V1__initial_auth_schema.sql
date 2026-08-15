CREATE TABLE auth_users (
    user_id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT uq_auth_users_email UNIQUE (email),
    CONSTRAINT chk_auth_users_status
        CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED'))
);

CREATE TABLE auth_credentials (
    user_id UUID PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    algorithm VARCHAR(32) NOT NULL,
    password_updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_auth_credentials_user
        FOREIGN KEY (user_id)
        REFERENCES auth_users (user_id)
);

CREATE TABLE auth_sessions (
    session_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    client_id UUID NOT NULL,
    refresh_token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,

    CONSTRAINT fk_auth_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES auth_users (user_id)
);

CREATE INDEX idx_auth_sessions_user_id
    ON auth_sessions (user_id);

CREATE INDEX idx_auth_sessions_refresh_token_hash
    ON auth_sessions (refresh_token_hash);

CREATE TABLE auth_outbox_events (
    event_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT chk_auth_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX idx_auth_outbox_pending
    ON auth_outbox_events (status);