ALTER TABLE projects
    ADD COLUMN next_issue_number BIGINT NOT NULL DEFAULT 1,
    ADD CONSTRAINT ck_projects_next_issue_number_positive CHECK (next_issue_number > 0);

ALTER TABLE issues
    ADD COLUMN issue_number BIGINT,
    ADD COLUMN rank_value BIGINT;

WITH numbered AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY project_id ORDER BY created_at, id) AS issue_number,
           ROW_NUMBER() OVER (PARTITION BY project_id, status ORDER BY created_at, id) * 1024 AS rank_value
    FROM issues
)
UPDATE issues AS issue
SET issue_number = numbered.issue_number,
    rank_value = numbered.rank_value
FROM numbered
WHERE issue.id = numbered.id;

UPDATE projects AS project
SET next_issue_number = COALESCE((
    SELECT MAX(issue.issue_number) + 1
    FROM issues AS issue
    WHERE issue.project_id = project.id
), 1);

ALTER TABLE issues
    ALTER COLUMN issue_number SET NOT NULL,
    ALTER COLUMN rank_value SET NOT NULL,
    ADD CONSTRAINT ck_issues_issue_number_positive CHECK (issue_number > 0),
    ADD CONSTRAINT uq_issues_project_issue_number UNIQUE (project_id, issue_number);

CREATE INDEX ix_issues_project_status_rank_active
    ON issues (project_id, status, rank_value, id)
    WHERE archived_at IS NULL;
