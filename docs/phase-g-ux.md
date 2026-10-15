# Phase G: Frontend User Experience

## Delivered workflows

- Registration and sign-in use browser validation, pending states, field-specific registration errors, and useful authentication feedback. Protected routes return signed-out users to sign-in.
- The home screen supports workspace selection/creation, member management by role, project creation/edit/archive, and profile updates.
- Project cards show project details and a searchable, sortable, filterable, paginated issue list. Members can create and edit issues, including status, priority, due date, and assignee. Owners/admins can archive and restore issues.
- Expanding an issue opens its details, comments, and activity history. Comments can be added while the issue and project are active.
- The notification panel shows unread state and lets the signed-in user mark their own notifications read.
- Loading, empty, success, and error states are provided in the authentication, workspace, issue, collaboration, and notification flows. Layouts adapt for tablet and narrow mobile screens.

## Manual acceptance checklist

1. Register a user, sign in, sign out, and confirm protected routes return to sign-in.
2. Create two workspaces and switch between them. Confirm projects and members stay scoped to the selected workspace.
3. Create a project, add a workspace member, create an issue, assign it, and use the search, filters, sort order, and pagination.
4. Open issue details, add a comment as a member, and confirm the comment and activity appear. Confirm assignment/comment notifications appear for the intended recipient and can be marked read.
5. Try an invalid status jump and confirm the form only offers valid next states while the backend still rejects invalid requests.
6. Archive/restore an issue as an owner/admin and confirm it leaves the default list. Confirm archived projects show issue data as read-only.
7. Repeat the primary navigation and issue workflows at desktop, tablet, and mobile widths; confirm forms and notification panel fit the viewport.

## Verification

The frontend production build and Oxlint pass. Backend Testcontainers integration tests cover the protected API behavior. The repository does not yet include a dedicated React component test runner; use the checklist above for browser interaction checks until UI testing infrastructure is added.
