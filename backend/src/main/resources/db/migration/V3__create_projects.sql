CREATE TABLE projects (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    created_by UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    project_key VARCHAR(10) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_projects_workspace FOREIGN KEY (workspace_id)
        REFERENCES workspaces (id) ON DELETE CASCADE,
    CONSTRAINT fk_projects_created_by FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uq_projects_workspace_key UNIQUE (workspace_id, project_key),
    CONSTRAINT ck_projects_name_not_blank CHECK (LENGTH(BTRIM(name)) > 0),
    CONSTRAINT ck_projects_key_format CHECK (project_key ~ '^[A-Z][A-Z0-9]{1,9}$'),
    CONSTRAINT ck_projects_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX ix_projects_workspace_status_name ON projects (workspace_id, status, name);
