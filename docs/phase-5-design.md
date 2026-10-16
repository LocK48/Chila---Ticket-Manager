# Phase 5: Reports and dashboards

Phase 5 adds project-level reporting backed by persisted issue and sprint data. The API is project-member scoped, following the same workspace membership rule as the project's issue and sprint APIs.

## Available reports

- **Project dashboard** (`GET /api/v1/projects/{projectId}/reports`): current unarchived issue counts by status, overdue count, activity-based completions in the last 30 days, average lead and cycle times, and up to 20 recent closed sprints.
- **Sprint report** (`GET /api/v1/projects/{projectId}/reports/sprints/{sprintId}`): daily burndown by issue count, final completion counts and percentage, and the issue status snapshot at sprint completion.
- The project **Overview** tab presents the dashboard. **Reports** selects a sprint and displays its burndown and completion snapshot.

## Metric definitions and limits

- Counts are issues, not story points; the application does not yet have an effort estimate field.
- Lead time is the elapsed time from issue creation to its first recorded transition to `DONE`.
- Cycle time is the elapsed time from its first recorded transition to `IN_PROGRESS` through its first recorded transition to `DONE`; transitions must be in that order. Averages use at most the 100 most recently completed, unarchived issues. The 30-day completion count counts distinct issues with a recorded transition to `DONE` in that window.
- Sprint scope history is captured at sprint start, on active-sprint scope changes, on status changes, and at completion. Burndown is the number of in-scope issues not in `DONE` per local calendar day (Asia/Bangkok), and scope reflects additions/removals. The chart is capped to 90 days.
- Sprint completion and velocity counts come from the completion snapshot. Older closed sprints have no fabricated/backfilled history and are marked unavailable. Sprints that have not been completed have no completion snapshot.
- Cycle/lead history uses existing issue activity rows. Transitions made before activity recording was available cannot be reconstructed.

## Persistence and migration

Flyway V10 adds `sprint_report_events`, a compact snapshot/event table containing sprint ID, issue identity/key/title/status, scope membership, event kind, and timestamp. Issue identifiers are intentionally historical snapshot values rather than foreign keys to mutable issue rows. Sprint deletion cascades its report rows. Existing issue, activity, and sprint data is preserved; no backfill is attempted because current values cannot accurately reconstruct past sprint scope.

The event table has indexes for sprint chronological reads and per-issue event reads. Dashboard status counts use the existing issue indexes and flow metrics are aggregated in PostgreSQL. Sprint report reads are bounded in practice by a 90-day chart window, although older event rows are retained to reconstruct the window's starting state.

## Verification

The PostgreSQL integration test exercises migration V10, member-authorized dashboard retrieval, sprint start/completion snapshots, burndown output, and velocity counts. Frontend production build validates the typed API and report views. Browser interaction verification remains a manual follow-up.
