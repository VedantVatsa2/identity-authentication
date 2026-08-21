CREATE TABLE auth_verification_challenges (
    challenge_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    challenge_type VARCHAR(32) NOT NULL,
    otp_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,

    CONSTRAINT fk_auth_verification_challenges_user
        FOREIGN KEY (user_id)
        REFERENCES auth_users (user_id),

    CONSTRAINT chk_auth_verification_challenge_type
        CHECK (challenge_type IN ('EMAIL_VERIFICATION')),

    CONSTRAINT chk_auth_verification_challenge_status
        CHECK (status IN ('PENDING', 'VERIFIED', 'EXPIRED', 'LOCKED')),

    CONSTRAINT chk_auth_verification_challenge_attempts
        CHECK (attempts >= 0)
);

CREATE INDEX idx_auth_verification_challenges_user_id
    ON auth_verification_challenges (user_id);

CREATE INDEX idx_auth_verification_challenges_user_status
    ON auth_verification_challenges (user_id, status);

CREATE INDEX idx_auth_verification_challenges_expires_at
    ON auth_verification_challenges (expires_at);