CREATE TABLE email_verification_codes (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users (id) ON DELETE CASCADE,
    email VARCHAR(320) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
    attempts SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    CONSTRAINT ck_email_verification_purpose CHECK (purpose IN ('REGISTRATION','PASSWORD_CHANGE')),
    CONSTRAINT ck_email_verification_attempts CHECK (attempts BETWEEN 0 AND 5),
    CONSTRAINT ck_email_verification_identity CHECK (
        (purpose = 'REGISTRATION' AND user_id IS NULL) OR
        (purpose = 'PASSWORD_CHANGE' AND user_id IS NOT NULL)
    )
);

CREATE INDEX ix_email_verification_email_purpose_created ON email_verification_codes (email, purpose, created_at DESC);
CREATE INDEX ix_email_verification_user_purpose_created ON email_verification_codes (user_id, purpose, created_at DESC);
