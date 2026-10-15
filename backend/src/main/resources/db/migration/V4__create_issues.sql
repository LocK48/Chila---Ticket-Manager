CREATE TABLE issues (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    reporter_id UUID NOT NULL,
    assignee_id UUID,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(10000),
    type VARCHAR(16) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    due_date DATE,
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_issues_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_issues_reporter FOREIGN KEY (reporter_id)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_issues_assignee FOREIGN KEY (assignee_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_issues_title_not_blank CHECK (LENGTH(BTRIM(title)) > 0),
    CONSTRAINT ck_issues_type CHECK (type IN ('TASK', 'BUG', 'STORY')),
    CONSTRAINT ck_issues_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE')),
    CONSTRAINT ck_issues_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'))
);

CREATE INDEX ix_issues_project_status_created ON issues (project_id, status, created_at DESC);
CREATE INDEX ix_issues_project_assignee ON issues (project_id, assignee_id);
CREATE INDEX ix_issues_project_due_date ON issues (project_id, due_date);
