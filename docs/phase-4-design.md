# Phase 4: workflow configuration and saved filters

## Workflow model

The application keeps the four existing issue statuses (`TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`) as its status catalog. This preserves existing issue records and API clients. Phase 4 configures project-specific directed transitions between those statuses; it does not introduce custom status creation or a general workflow engine.

Projects with no workflow configuration use the established safe defaults:

- TODO → IN_PROGRESS
- IN_PROGRESS → TODO or IN_REVIEW
- IN_REVIEW → IN_PROGRESS or DONE
- DONE → IN_PROGRESS

An owner or workspace administrator can replace the entire transition set, including disabling all status changes. The backend enforces the selected transitions for issue edits and board moves. Same-status updates remain valid. Existing status activity records continue to be written by issue updates and board moves. Only owners/admins can read or modify the workflow settings page's write API; members can read the workflow and continue to work within its transition rules.

`workflow_configs` distinguishes an intentionally empty transition set from a project that has never been customized. `workflow_transitions` stores allowed edges and prevents duplicate/self transitions. Both are project-owned and cascade with project deletion. V9 is additive and keeps the existing status values and issue rows intact.

API:

- `GET /api/v1/projects/{projectId}/workflow`
- `PUT /api/v1/projects/{projectId}/workflow/transitions` with a JSON array of `{ "from": "TODO", "to": "IN_PROGRESS" }`

## Saved filters

Filters are project-scoped and store typed criteria rather than query expressions. Supported criteria are status, priority, issue type, assignee/unassigned, sprint, epic, due-date range, and text search. The issue list endpoint now accepts `sprintId`, `epicId`, `dueFrom`, and `dueTo` in addition to its previous filters. Labels are not supported by the issue model yet.

Each filter is private to its creator or shared with project/workspace members. Any member can create a filter. Only its creator can edit or delete it. Workspace-visible filters are returned only after project membership is checked. Sprint and epic identifiers are validated to belong to the same project; assignees must be workspace members. Criteria are passed as typed query parameters and never interpreted as SQL.

API:

- `GET/POST /api/v1/projects/{projectId}/filters`
- `PUT/DELETE /api/v1/projects/{projectId}/filters/{filterId}`
- `GET /api/v1/projects/{projectId}/issues` accepts saved-filter criteria as normal typed query parameters.

The issue page lets users save the current criteria and select a saved filter. Sharing is a deliberate choice at save time.

## Authorization review

- Workflow configuration writes: workspace owner/admin only; archived projects are read-only.
- Filter creation/read: project workspace members.
- Shared-filter visibility: members who can access that project.
- Filter update/delete: creator only, after project membership and project scope are confirmed.
- Issue mutation: existing membership, project-state, archived-issue, sprint-history, and workflow checks remain enforced server-side.
- No bulk mutation endpoint was added because this phase does not establish a concrete bulk-edit workflow or atomicity contract.

## Limitations and verification

Statuses themselves remain fixed; customization covers transitions only. Filter criteria do not yet include labels, and the current issue domain has no label model. Saved filter definitions are applied through the issue list query rather than compiled into a separate query language. V9 has no destructive rollback, because downgrade would discard user workflow and filter data.

Frontend lint and production build pass. Backend main/test sources compile, and the five existing non-container unit tests pass. The Phase 4 Testcontainers integration test is present and compiles, but PostgreSQL migration/integration execution still needs Docker Engine. Browser interaction checks have not been performed.
