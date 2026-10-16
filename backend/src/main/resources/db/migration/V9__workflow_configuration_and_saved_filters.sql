CREATE TABLE workflow_configs (
    project_id UUID PRIMARY KEY REFERENCES projects(id) ON DELETE CASCADE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE workflow_transitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES workflow_configs(project_id) ON DELETE CASCADE,
    from_status VARCHAR(24) NOT NULL,
    to_status VARCHAR(24) NOT NULL,
    CONSTRAINT ck_workflow_transition_status CHECK (from_status IN ('TODO','IN_PROGRESS','IN_REVIEW','DONE') AND to_status IN ('TODO','IN_PROGRESS','IN_REVIEW','DONE')),
    CONSTRAINT ck_workflow_transition_distinct CHECK (from_status <> to_status),
    CONSTRAINT uq_workflow_transition UNIQUE (project_id, from_status, to_status)
);
CREATE INDEX ix_workflow_transitions_project ON workflow_transitions(project_id, from_status);

CREATE TABLE saved_filters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    visibility VARCHAR(16) NOT NULL DEFAULT 'PRIVATE',
    status VARCHAR(24),
    priority VARCHAR(24),
    type VARCHAR(24),
    assignee_id UUID REFERENCES users(id) ON DELETE SET NULL,
    unassigned BOOLEAN NOT NULL DEFAULT FALSE,
    sprint_id UUID REFERENCES sprints(id) ON DELETE SET NULL,
    epic_id UUID REFERENCES epics(id) ON DELETE SET NULL,
    due_from DATE,
    due_to DATE,
    search VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_saved_filter_visibility CHECK (visibility IN ('PRIVATE','WORKSPACE')),
    CONSTRAINT ck_saved_filter_assignee CHECK (NOT (unassigned AND assignee_id IS NOT NULL)),
    CONSTRAINT ck_saved_filter_dates CHECK (due_from IS NULL OR due_to IS NULL OR due_from <= due_to)
);
CREATE INDEX ix_saved_filters_project_visibility ON saved_filters(project_id, visibility);
CREATE INDEX ix_saved_filters_creator ON saved_filters(created_by);
