# Phase 6: Project automation

Phase 6 begins with one small, safe automation workflow: project admins can configure a rule for an issue entering a selected status. The action creates a notification for the issue reporter or assignee.

## Rule behavior

- Rules are project-scoped and may be created, edited, enabled/disabled, listed, and deleted by workspace owners/admins. Project members can read rules and execution history.
- A rule runs only on an actual issue status transition, through either issue editing or board moves. Creating an issue directly in a status does not count as a transition.
- Rules run in creation order. The first release supports `NOTIFY_REPORTER` and `NOTIFY_ASSIGNEE` actions and caps a project at 50 rules.
- Missing recipients and users who are no longer workspace members produce a `SKIPPED` execution record. Successful notification delivery produces `SUCCEEDED`.
- Rule deletion removes the rule but retains its executions, including the rule name and issue key snapshots.

## Failure and loop behavior

Execution is synchronous and transactional because it performs only short database writes. Notification creation, execution logging, and the issue status change commit together. A database failure rolls back the status transition and the automation records. No external calls are made from an HTTP request. Rules cannot change status, assignment, or other rule-triggering fields; notifications are the only supported action, so recursive loops are not possible.

## Authorization and persistence

Flyway V11 adds `automation_rules` and `automation_executions`. Rule mutations require an active project and a workspace owner/admin. Rule reads and execution history require workspace membership. Rule queries are scoped to the selected project, and issue notifications use the existing per-user notification authorization.

Executions retain snapshots of the rule name and issue key. Deleting a rule nulls its execution rule reference without deleting the history. Deleting a project cascades its automation rules and execution logs. No existing project or issue data is altered.

## API

- `GET /api/v1/projects/{projectId}/automation/rules`
- `POST /api/v1/projects/{projectId}/automation/rules`
- `PUT /api/v1/projects/{projectId}/automation/rules/{ruleId}`
- `DELETE /api/v1/projects/{projectId}/automation/rules/{ruleId}`
- `GET /api/v1/projects/{projectId}/automation/executions` (latest 50)

The project settings page includes rule management and recent execution history. Integrations, scheduled triggers, outbound email/webhooks, assignment actions, and background queues remain future work.

## Verification

The PostgreSQL integration suite verifies the migration, a status-triggered notification, execution history, and retained execution data after rule deletion. Frontend build and lint verify the settings UI and typed API calls. Manual browser verification remains a follow-up.
