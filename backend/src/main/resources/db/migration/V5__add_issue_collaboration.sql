CREATE TABLE issue_comments (
    id UUID PRIMARY KEY,
    issue_id UUID NOT NULL REFERENCES issues (id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    body VARCHAR(5000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_issue_comments_body_not_blank CHECK (LENGTH(BTRIM(body)) > 0)
);

CREATE INDEX ix_issue_comments_issue_created ON issue_comments (issue_id, created_at DESC);

CREATE TABLE issue_activities (
    id UUID PRIMARY KEY,
    issue_id UUID NOT NULL REFERENCES issues (id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    action VARCHAR(40) NOT NULL,
    field_name VARCHAR(40),
    old_value VARCHAR(2000),
    new_value VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_issue_activities_issue_created ON issue_activities (issue_id, created_at DESC);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    issue_id UUID REFERENCES issues (id) ON DELETE SET NULL,
    event_type VARCHAR(24) NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMPTZ
);

CREATE INDEX ix_notifications_recipient_created ON notifications (recipient_id, created_at DESC);
CREATE INDEX ix_notifications_recipient_unread ON notifications (recipient_id, created_at DESC)
    WHERE read_at IS NULL;
