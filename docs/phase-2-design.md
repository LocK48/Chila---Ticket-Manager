# Phase 2: backlog and Scrum sprints

## Data model

Migration V7 adds `sprints` and optional `issues.sprint_id`. A null sprint means the issue is in the product backlog; workflow status remains independent. Each issue also has a `backlog_rank`, separate from its status-column `rank_value`. V7 backfills backlog order by issue creation time without changing issue UUIDs, keys, or current workflow ranking.

A sprint belongs to exactly one project and moves `FUTURE → ACTIVE → CLOSED`. A partial unique index permits at most one active sprint per project. The project row lock serializes sprint lifecycle changes, issue planning, and issue creation/reordering for the project. A sprint can be started only after it has an end date and is on or after its planned start date. Dates must be ordered. Managers (workspace owners/admins) create, edit, start, and complete sprints; any project member may plan issues, subject to project and sprint state checks.

Completed issues remain attached to the closed sprint as historical scope. On completion, the manager explicitly selects where unfinished issues go: the backlog or another future sprint. This operation updates issue membership and closes the sprint in one transaction. Issues in closed sprints cannot be reassigned or have their workflow status changed, preserving the completion record.

## API

- `GET/POST /api/v1/projects/{projectId}/sprints`
- `PATCH /api/v1/projects/{projectId}/sprints/{sprintId}` for future sprint details.
- `POST /api/v1/projects/{projectId}/sprints/{sprintId}/start`
- `POST /api/v1/projects/{projectId}/sprints/{sprintId}/complete` with `unfinishedIssueAction` set to `BACKLOG` or `FUTURE_SPRINT` and `targetSprintId` required only for the latter.
- `GET /api/v1/projects/{projectId}/sprints/{sprintId}/issues` returns a filtered, paginated status column.
- `POST .../sprints/{sprintId}/issues` plans up to 100 selected issues transactionally; `POST .../remove-issues` returns a selection to the backlog.
- `GET /api/v1/projects/{projectId}/backlog` returns filtered, paginated backlog issues ordered by `backlog_rank`.
- `PATCH /api/v1/issues/{issueId}/sprint` assigns an issue to a future/active sprint or returns it to the backlog, including a `beforeIssueId` for backlog reordering.

Every endpoint checks project workspace membership. Sprint lifecycle changes require owner/admin access. Sprint and issue associations are project-scoped, so a sprint ID from another project is not accepted. Archived projects/issues and closed sprint membership cannot be changed.

## UI and current limits

Project tabs include Backlog and Active sprint. The backlog supports issue-key/text, assignee, priority, and type filters; issue creation; per-issue ordering controls; selection; and bulk sprint planning. Future sprints can be created/edited and started. The active sprint view reuses the Kanban board and allows selection removal, adding backlog items, and explicit unfinished-work routing at completion. Closed sprints remain listed in history.

Backlog and sprint issue lists are paginated (maximum page size 100); the current planning selectors load at most the first 100 options at once. The active sprint summary likewise uses up to 100 issues; each board status column pages independently. There is no story-point field or burndown/velocity report. Frontend tests are not configured in this project; validate the screens in a browser after the build.

## Verification

Backend integration tests cover sprint creation, planning, active board pagination, one-active-sprint enforcement, member permission denial, completion to backlog, and closed history. They use PostgreSQL Testcontainers and require Docker Desktop. Run `./mvnw.cmd test` from `backend`; run `npm run build` and `npm run lint` from `frontend`.

In the latest phase review, backend compilation and the five Docker-independent unit tests passed. The Docker-backed integration suite and manual browser review remain pending because Docker Desktop's engine endpoint is unavailable in the current environment.
