CREATE TABLE epics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    summary VARCHAR(2000),
    color VARCHAR(7) NOT NULL DEFAULT '#6554C0',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_epics_project_name UNIQUE (project_id, name),
    CONSTRAINT ck_epics_color CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);
CREATE INDEX ix_epics_project ON epics(project_id, name);

CREATE TABLE releases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(2000),
    start_date DATE,
    release_date DATE,
    state VARCHAR(16) NOT NULL DEFAULT 'UNRELEASED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_releases_project_name UNIQUE (project_id, name),
    CONSTRAINT ck_releases_state CHECK (state IN ('UNRELEASED', 'RELEASED', 'ARCHIVED')),
    CONSTRAINT ck_releases_dates CHECK (start_date IS NULL OR release_date IS NULL OR start_date <= release_date)
);
CREATE INDEX ix_releases_project_timeline ON releases(project_id, start_date, release_date);

ALTER TABLE issues ADD COLUMN epic_id UUID REFERENCES epics(id) ON DELETE SET NULL;
ALTER TABLE issues ADD COLUMN parent_issue_id UUID REFERENCES issues(id) ON DELETE SET NULL;
ALTER TABLE issues ADD COLUMN release_id UUID REFERENCES releases(id) ON DELETE SET NULL;
CREATE INDEX ix_issues_epic ON issues(epic_id) WHERE epic_id IS NOT NULL;
CREATE INDEX ix_issues_parent ON issues(parent_issue_id) WHERE parent_issue_id IS NOT NULL;
CREATE INDEX ix_issues_release ON issues(release_id) WHERE release_id IS NOT NULL;

CREATE TABLE issue_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    target_issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    link_type VARCHAR(20) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_issue_links_not_self CHECK (source_issue_id <> target_issue_id),
    CONSTRAINT ck_issue_links_type CHECK (link_type IN ('BLOCKS', 'DEPENDS_ON', 'RELATES_TO')),
    CONSTRAINT uq_issue_links_pair_type UNIQUE (source_issue_id, target_issue_id, link_type)
);
CREATE INDEX ix_issue_links_target ON issue_links(target_issue_id, link_type);
