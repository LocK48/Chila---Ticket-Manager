# Phase 3: epics, issue hierarchy, and releases

## Architectural decisions

- Epics are a separate project-owned entity. They are not encoded as special issues, keeping issue types and issue workflows unchanged.
- Parent-child hierarchy is a nullable `issues.parent_issue_id` self-reference. It is independent of links so that hierarchy can enforce a strict acyclic tree while links express dependencies or context.
- Directed issue links live in `issue_links`, with `BLOCKS`, `DEPENDS_ON`, and `RELATES_TO` types. “Is blocked by” and “is required by” are displayed as the inverse of the saved direction. A pair cannot link to itself.
- Releases are project-owned versions with dates and `UNRELEASED`, `RELEASED`, or `ARCHIVED` state. The timeline is intentionally lightweight.
- Epic, parent, link, and release associations are checked against the same project in the service layer. Project/workspace membership is checked on every API read and write; owners/admins manage epics and releases, while project members can associate issues and manage issue links.
- Relationship changes are recorded in issue activity. Deleting an epic or release at the database level leaves issues unassigned; deleting an issue removes its links and clears children’s parent references. The application does not expose destructive delete actions for epics or releases yet.

## Migration

V8 adds project-scoped `epics` and `releases`, then nullable `epic_id`, `parent_issue_id`, and `release_id` columns to `issues`. Existing issues are preserved and start without these associations. `issue_links` has foreign keys, self-link/type checks, and a uniqueness constraint. The migration is additive. No rollback SQL is provided; downgrade would require intentionally dropping the new data and objects.

## API

- `GET/POST /api/v1/projects/{projectId}/epics`; `PUT /api/v1/projects/{projectId}/epics/{epicId}`.
- `GET /api/v1/projects/{projectId}/epics/{epicId}/issues` supports status, text search, and pagination.
- `GET /api/v1/projects/{projectId}/timeline/due-issues` returns project issues with due dates, ordered by date and paginated.
- `GET/POST /api/v1/projects/{projectId}/releases`; `PUT /api/v1/projects/{projectId}/releases/{releaseId}`.
- `PUT /api/v1/issues/{issueId}/epic`, `/parent`, and `/release` accept `{ "id": UUID | null }` to assign or clear a relation.
- `GET /api/v1/issues/{issueId}/relationships` returns parent, children, and links.
- `POST /api/v1/issues/{issueId}/links` adds a directed link; `DELETE /api/v1/issues/{issueId}/links/{linkId}` removes it.

Issue associations remain within a project. Parent changes walk the proposed parent chain and reject a cycle. Existing issue UUID routes and key behavior remain unchanged.

## UI

Project tabs provide an epic manager with issue counts/progress and searchable/status-filtered epic issues, plus a lightweight planning page that shows epic workstreams, up to 100 due-dated issues, and releases with dates, state, and associated issue counts. Issue details can assign an epic or release, set/remove a parent, review children and links, and add/remove links by issue key.

## Phase 2 audit

The current code covers the Phase 2 acceptance criteria: independent backlog membership/order; future/active/closed sprint transitions; scoped issue planning; active board reuse; and explicit routing for unfinished work with completed issues retained in closed history. The complete backend test suite now passes against PostgreSQL 17 through Testcontainers, including sprint lifecycle, role, cross-project, and history assertions. Browser interaction verification remains outstanding.

## Verification and current limits

The full backend test suite (32 tests) passes against PostgreSQL 17 with Testcontainers. Browser verification remains outstanding. Epic lists currently calculate progress from issue counts, with completed meaning `DONE`; this is current status, not historical progress. Links are directed; dependencies do not block workflow transitions automatically. Epic records do not yet have schedule dates, so they appear as workstreams with progress rather than dated bars. The due-date list is capped at its first 100 results. Release dates are planning metadata, not a Gantt schedule. There are no delete endpoints or release/epic archival history snapshots.
