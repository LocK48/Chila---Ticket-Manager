CREATE TABLE sprints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    goal VARCHAR(2000),
    state VARCHAR(16) NOT NULL DEFAULT 'FUTURE',
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_sprints_state CHECK (state IN ('FUTURE', 'ACTIVE', 'CLOSED')),
    CONSTRAINT ck_sprints_date_order CHECK (start_date IS NULL OR end_date IS NULL OR start_date <= end_date)
);

CREATE INDEX ix_sprints_project_state ON sprints(project_id, state, created_at);
CREATE UNIQUE INDEX uq_sprints_one_active_per_project ON sprints(project_id) WHERE state = 'ACTIVE';

ALTER TABLE issues
    ADD COLUMN sprint_id UUID REFERENCES sprints(id) ON DELETE SET NULL,
    ADD COLUMN backlog_rank BIGINT;

WITH ordered_backlog AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY project_id ORDER BY created_at, id) * 1024 AS backlog_rank
    FROM issues
)
UPDATE issues AS issue SET backlog_rank = ordered_backlog.backlog_rank
FROM ordered_backlog WHERE issue.id = ordered_backlog.id;

ALTER TABLE issues ALTER COLUMN backlog_rank SET NOT NULL;
CREATE INDEX ix_issues_project_backlog_rank ON issues(project_id, backlog_rank, id)
    WHERE sprint_id IS NULL AND archived_at IS NULL;
CREATE INDEX ix_issues_sprint_status_rank ON issues(sprint_id, status, rank_value, id)
    WHERE sprint_id IS NOT NULL AND archived_at IS NULL;
