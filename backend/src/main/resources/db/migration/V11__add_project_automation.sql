CREATE TABLE automation_rules (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    target_status VARCHAR(24) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_automation_rule_status CHECK (target_status IN ('TODO','IN_PROGRESS','IN_REVIEW','DONE')),
    CONSTRAINT ck_automation_rule_action CHECK (action_type IN ('NOTIFY_REPORTER','NOTIFY_ASSIGNEE')),
    CONSTRAINT ck_automation_rule_name CHECK (LENGTH(BTRIM(name)) > 0)
);

CREATE INDEX ix_automation_rules_trigger ON automation_rules (project_id, enabled, target_status, created_at);

CREATE TABLE automation_executions (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    rule_id UUID REFERENCES automation_rules (id) ON DELETE SET NULL,
    rule_name VARCHAR(100) NOT NULL,
    issue_id UUID REFERENCES issues (id) ON DELETE SET NULL,
    issue_key VARCHAR(32) NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    detail VARCHAR(300) NOT NULL,
    executed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_automation_execution_outcome CHECK (outcome IN ('SUCCEEDED','SKIPPED'))
);

CREATE INDEX ix_automation_executions_project_time ON automation_executions (project_id, executed_at DESC);
CREATE INDEX ix_automation_executions_rule_time ON automation_executions (rule_id, executed_at DESC);
