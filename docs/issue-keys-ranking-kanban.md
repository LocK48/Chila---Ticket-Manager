# Issue keys, ranking, and Kanban

## Issue keys

Issues keep their UUID as the internal and API identity. Each project also has a stable, human-readable key of the form `PROJECT-123`. The project row stores the next number, and issue creation locks that row while allocating a number. This keeps numbers unique and sequential within a project when multiple members create issues concurrently. The database enforces uniqueness on `(project_id, issue_number)`. Existing issues receive numbers ordered by creation time and UUID during migration V6. Project keys are unique within a workspace, so key-based lookup is scoped to the workspace.

## Ranking

Each issue stores a `rank_value` within its project and status column. New issues append in increments of 1024. A move between cards chooses a value between neighboring ranks; moving to an end uses the adjacent rank step. When no integer gap remains, the destination column is rebalanced in increments of 1024. Reordering and status changes run in a transaction while holding the project row lock, so simultaneous board operations for one project serialize. The rank index excludes archived issues.

## API

- `GET /api/v1/workspaces/{workspaceId}/issues/{issueKey}` resolves a project key and issue number for a workspace. Existing UUID lookup remains supported.
- `PATCH /api/v1/issues/{issueId}/position` accepts `{ "status": "TODO", "beforeIssueId": "..." }`; omit `beforeIssueId` to append to that column. Existing transition rules are enforced.
- `GET /api/v1/projects/{projectId}/issues?sort=rank&direction=asc` provides rank-ordered issue pages. Search also matches issue keys.

All endpoints retain workspace membership checks; archived projects and archived issues cannot be moved. Issue response objects include `issueKey` and `issueNumber` while preserving their UUID fields.

## Board behavior

The project board has one column per existing issue status. It loads a bounded page per column and provides a per-column “Load more” action. Search, assignee, priority, and type filters combine. Cards support pointer drag-and-drop and keyboard controls for changing columns or moving earlier/later in a column. The board uses the existing issue API and does not add a drag-and-drop dependency.

The current board is a Kanban view. Backlog and sprint lifecycle are not implemented yet; they require separate sprint data and transition rules. The frontend project does not currently have an automated component-test framework, so board interactions still need manual browser verification.

## Migration and verification

Flyway V6 backfills issue numbers and initial per-status ranks, advances each project's number counter, adds constraints, and creates the active rank index. Validate with backend integration tests and the frontend build/lint commands described in the root README. The integration suite uses PostgreSQL Testcontainers and requires a working Docker Engine.
