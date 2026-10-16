CREATE TABLE sprint_report_events (
    event_id BIGSERIAL PRIMARY KEY,
    sprint_id UUID NOT NULL REFERENCES sprints(id) ON DELETE CASCADE,
    issue_id UUID NOT NULL,
    issue_key VARCHAR(32) NOT NULL,
    issue_title VARCHAR(200) NOT NULL,
    issue_status VARCHAR(24) NOT NULL,
    event_kind VARCHAR(24) NOT NULL,
    in_scope BOOLEAN NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_sprint_report_event_status CHECK (issue_status IN ('TODO','IN_PROGRESS','IN_REVIEW','DONE')),
    CONSTRAINT ck_sprint_report_event_kind CHECK (event_kind IN ('SPRINT_START','ISSUE_ADDED','ISSUE_REMOVED','STATUS_CHANGED','SPRINT_COMPLETED'))
);
CREATE INDEX ix_sprint_report_events_sprint_time ON sprint_report_events(sprint_id, occurred_at, event_id);
CREATE INDEX ix_sprint_report_events_issue ON sprint_report_events(sprint_id, issue_id, occurred_at, event_id);
