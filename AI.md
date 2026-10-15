# MASTER PROMPT — UPGRADE CHILA INTO A JIRA-LIKE PROJECT MANAGEMENT PLATFORM

## 1. Your Role

Act as a Senior Full-Stack Engineer, Java Spring Boot Architect, PostgreSQL Database Engineer, and Technical Lead.

Your task is to inspect, improve, and extend my existing project, **Chila**, into a professional Jira-like project and issue management platform.

You must work with the existing codebase rather than generating a completely new application.

Your responsibilities include:

- Understanding the current architecture and implementation.
- Identifying missing features and technical debt.
- Designing database migrations and API contracts.
- Implementing backend and frontend features.
- Maintaining authorization and data integrity.
- Writing automated tests.
- Improving the UI/UX.
- Documenting architectural decisions and setup instructions.

Work incrementally. Do not attempt to implement every feature in a single response or make large, unverified changes.

## 2. Existing Project Context

Chila is a team project and issue management application.

The current implementation already has the following capabilities:

- Workspaces.
- Projects.
- Issues.
- Issue types, statuses, and priorities.
- Issue assignees and due dates.
- Comments.
- Activity history.
- Notifications.
- Workspace membership and role-based permissions.
- A React + TypeScript frontend.
- A Java Spring Boot backend.
- PostgreSQL as the main database.

The existing frontend contains an issue panel at:

`frontend/src/features/issues/IssuesPanel.tsx`

The current API/authentication-related code includes:

`frontend/src/auth/api.ts`

These paths are starting points, not an exhaustive list of the architecture. Inspect the actual repository before deciding what to modify.

Do not assume every feature described above is fully implemented or production-ready. Verify its current behavior in the codebase.

## 3. Technology Constraints

Preserve the current stack unless a concrete technical reason justifies a change.

### Backend

- Java 21.
- Spring Boot.
- Spring Security.
- Spring Data JPA.
- PostgreSQL.
- Flyway for schema migrations, if already configured or introduced consistently.
- REST APIs.
- Bean Validation.
- JUnit 5.
- Mockito.
- Spring Boot Test.

### Frontend

- React.
- TypeScript.
- Vite.
- React Router.
- Existing API client and authentication implementation.
- Existing styling and UI component libraries where appropriate.

### Engineering rules

- Keep a modular monolith architecture.
- Do not introduce microservices.
- Do not replace Spring Boot with Node.js.
- Do not replace PostgreSQL with another database.
- Do not introduce Redis, Elasticsearch, Kafka, or other infrastructure without a demonstrated need.
- Do not remove existing functionality simply to simplify implementation.
- Do not duplicate existing components or API clients.
- Never store sensitive credentials in source code.
- Do not claim that code, migrations, builds, or tests succeeded unless they have actually been verified.

## 4. Product Vision

Evolve Chila from a basic project and issue manager into a Jira-like platform that supports two major ways of working:

### Kanban

Users can visualize workflow stages, move issues between statuses, filter work, identify bottlenecks, and monitor work in progress.

### Scrum

Users can organize a backlog, prioritize issues, plan sprints, run active sprints, and review sprint outcomes.

Later, extend the product with epics, issue hierarchies, releases, customizable workflows, reports, dashboards, and automation.

The objective is not to reproduce all of Jira. The objective is to implement a coherent subset of its most valuable workflows with good architecture and a realistic scope for a portfolio project.

## 5. Mandatory Workflow Before Coding

### Step 1 — Inspect the repository

Inspect:

- The root directory structure.
- Backend package structure.
- Existing JPA entities and relationships.
- Database migrations and schema.
- Controllers, services, repositories, and DTOs.
- Authentication and authorization.
- Existing issue lifecycle and status definitions.
- Frontend routing and layout.
- Existing issue, project, and workspace components.
- Existing API functions and TypeScript types.
- Existing tests and build configuration.

Start with the known issue panel and API files, then trace their dependencies into the rest of the codebase.

If repository access is unavailable, ask me for the relevant directory tree and files. Never claim to have inspected inaccessible files.

### Step 2 — Produce an implementation assessment

Create a table with these columns:

| Feature | Current state | Missing work | Dependencies | Priority |
| ------- | ------------- | ------------ | ------------ | -------- |

Classify each feature as:

- Implemented and verified.
- Partially implemented.
- Missing.
- Requires investigation.

Identify existing implementation decisions that must be preserved.

### Step 3 — Create an incremental roadmap

Break the work into milestones with:

- Scope.
- Database changes.
- Backend changes.
- Frontend changes.
- Authorization implications.
- Tests.
- Acceptance criteria.

Before implementation, show me the proposed plan and identify any major design decisions that need approval.

After the plan is approved, begin with Phase 1. Do not stop after producing a plan unless access to the repository or an important decision is missing.

---

# PHASE 1 — ISSUE IDENTIFIERS, RANKING, AND KANBAN BOARD

This is the first implementation priority.

## 6. Human-Readable Issue Keys

Currently, issues are primarily identified using UUIDs. Add human-readable issue identifiers such as:

- `WEB-123`
- `API-42`
- `CHILA-8`

Requirements:

- Every issue has a stable, human-readable key.
- The key is unique within the defined project scope.
- The key does not change when the issue is edited, moved between statuses, or assigned to another user.
- The existing UUID remains the internal primary key unless there is a strong reason to change it.
- The frontend uses human-readable keys in issue links, issue details, tables, and board cards.
- The backend supports looking up an issue by key.
- UUID-based API routes remain compatible where needed.

Use the project's key, such as `WEB`, as the prefix if this is compatible with the existing project model.

Define the key-generation strategy carefully.

For concurrent issue creation:

- Prevent duplicate keys using database constraints.
- Do not calculate the next number using an unsafe `COUNT(*) + 1` strategy.
- Use an appropriate transactional counter, database sequence, or equivalent concurrency-safe design.
- Define how project keys are created, validated, and changed.
- If project keys can change, determine how existing issue keys behave. Prefer immutable issue keys once created.

If the database already contains issues, create a migration or backfill strategy that assigns keys without losing data or violating uniqueness.

Test concurrent creation and collision handling where practical.

## 7. Issue Ranking and Ordering

Add a persistent ordering mechanism for issues.

Users must be able to reorder issues manually without relying on creation date or updated date.

Requirements:

- Introduce a ranking or ordering field appropriate for PostgreSQL.
- Support stable ordering within a project and relevant board or backlog scope.
- Persist drag-and-drop ordering.
- Support moving an issue between status columns.
- Support moving an issue between backlog and sprint in later phases.
- Handle concurrent updates safely.
- Ensure pagination and sorting behave consistently.
- Avoid reordering every issue unnecessarily after each drag operation.

Evaluate an appropriate ranking strategy, such as sparse integer ranks or lexicographic/fractional ranks. Choose the simplest reliable design for the expected project size and explain the trade-offs.

Define how ranking interacts with filters, status columns, sprint membership, and backlog ordering.

Do not make drag-and-drop purely visual. Successful reordering must persist to the backend.

## 8. Kanban Board

Implement a Kanban board for a project.

Suggested route:

`/workspaces/:workspaceId/projects/:projectId/board`

Adapt the route to the existing routing conventions.

### Board columns

Use the existing issue status model. Do not create a separate incompatible status enum for the board.

Display issues grouped by status.

If the existing status model does not map cleanly to Kanban columns, document the required change before implementing it.

### Issue cards

Each card should display relevant information:

- Human-readable issue key.
- Issue title.
- Issue type.
- Priority.
- Assignee avatar or initials.
- Due date when present.
- Optional labels if already supported.

Make cards compact and easy to scan.

### Drag and drop

Users should be able to:

- Move an issue to another status column.
- Reorder an issue within the same column.
- Move issues across columns.
- See clear drag-over and drop states.

Use a maintained drag-and-drop library compatible with the existing frontend, or another accessible implementation with a clear technical justification.

Requirements:

- Persist status changes and ranking changes through the backend.
- Avoid optimistic UI updates that leave the interface inconsistent after an API failure.
- If optimistic updates are used, implement rollback or reliable state reconciliation.
- Prevent unauthorized users from performing mutations.
- Avoid accidental drag activation when interacting with buttons or menus inside a card.
- Support keyboard-accessible alternatives where practical.

### Quick filters

Add useful filters:

- Assignee.
- Priority.
- Issue type.
- Due date, if useful.
- Search by issue key or title.

Provide a way to clear filters.

Filters must compose correctly. Applying one filter must not silently discard the others.

### Swimlanes

Support grouping by assignee as an optional board view.

Provide a simple selector for:

- No swimlanes.
- Group by assignee.

Define where unassigned issues appear.

Do not make swimlanes mandatory if they create significant complexity before the board is stable.

### Board UX

Include:

- Project title and key.
- Board heading.
- Issue count per column.
- Create issue action.
- Loading state.
- Error state.
- Empty column state.
- No matching filter results state.
- Responsive horizontal scrolling.
- Clear keyboard focus and accessible controls.

Avoid loading an unbounded number of issues. If necessary, introduce a clear pagination or incremental-loading strategy.

## 9. Workspace and Project Issue APIs

Inspect the existing API design before adding endpoints.

Provide a consistent way to query issues by project and workspace.

Potential API capabilities include:

- List issues for a project.
- List issues accessible within a workspace.
- Filter by status, assignee, priority, type, and text query.
- Sort by ranking or another supported field.
- Paginate results.
- Retrieve an issue by human-readable key.
- Update issue status.
- Update issue rank or order.

Do not blindly implement these exact paths if the current API already provides equivalent capabilities.

Define request parameters, response DTOs, pagination metadata, and error responses.

Ensure every query respects workspace membership and project authorization. A guessed UUID or issue key must not bypass access control.

Avoid N+1 queries when loading assignees, projects, or other card details.

### Phase 1 acceptance criteria

Phase 1 is complete when:

1. Existing issues retain their data.
2. Each issue has a unique, stable, human-readable key.
3. Issues can be persistently reordered.
4. The Kanban board displays issues in their correct status columns.
5. Drag-and-drop updates the backend and survives page refresh.
6. Quick filters work together correctly.
7. Assignee swimlanes work if included in the initial implementation.
8. Unauthorized users cannot modify issues or access private project data.
9. Backend and frontend builds pass.
10. Relevant automated tests pass.
11. The feature is documented.

---

# PHASE 2 — BACKLOG AND SCRUM SPRINTS

Implement this phase only after Phase 1 is stable.

## 10. Product Backlog

Add a backlog view for a project.

Suggested route:

`/workspaces/:workspaceId/projects/:projectId/backlog`

The backlog contains issues that are not assigned to an active or planned sprint, according to the chosen data model.

Features:

- View backlog issues.
- Sort and reorder backlog issues.
- Create issues from the backlog.
- Search and filter backlog issues.
- Select issues for sprint planning.
- Move issues into a sprint.
- Remove issues from a sprint and return them to the backlog.
- Display issue type, key, priority, and assignee.
- Preserve ordering after page refresh.

Clearly define whether issues can exist without a sprint and how completed sprint issues are handled.

Do not infer backlog membership solely from an issue status. Backlog membership and workflow status are separate concepts.

## 11. Sprint Data Model

Design a `Sprint` entity related to a project.

Suggested fields:

- `id`
- `project_id`
- `name`
- `goal`
- `state`
- `start_date`
- `end_date`
- `created_at`
- `updated_at`

Suggested sprint states:

- `FUTURE`
- `ACTIVE`
- `CLOSED`

Use the existing project's conventions where appropriate.

Define and enforce business rules:

- A sprint belongs to exactly one project.
- Issues cannot be assigned to a sprint belonging to another project.
- A project should not have multiple active sprints unless multiple active sprints are explicitly supported by the design.
- Start and end dates must be valid.
- The sprint goal is optional.
- Only authorized users can create, start, complete, or modify sprints.
- Closed sprint history must be preserved.
- Moving issues between sprints must respect the issue's project and authorization rules.

Use database constraints and transactional service logic where appropriate.

## 12. Sprint Planning and Management

Implement:

- Create sprint.
- Edit sprint details.
- Add issues to a sprint.
- Remove issues from a sprint.
- Reorder sprint issues.
- Start sprint.
- Complete sprint.
- View sprint details.
- View previous sprints.

Starting a sprint should validate the chosen dates and ensure the project has no conflicting active sprint under the selected business rules.

Completing a sprint must define what happens to unfinished issues. Provide a clear choice to move incomplete issues to the backlog or another sprint, and preserve completed sprint history.

Do not silently delete or lose issue assignments during sprint completion.

All operations that modify multiple related records must be transactional where appropriate.

## 13. Active Sprint Board

Create an active sprint page that reuses the Kanban board implementation where possible.

Suggested route:

`/workspaces/:workspaceId/projects/:projectId/active-sprint`

Include:

- Active sprint name and goal.
- Sprint start and end dates.
- Issues grouped by workflow status.
- Issue drag-and-drop.
- Remaining and completed issue counts.
- Sprint completion action.
- Link back to the backlog.
- Appropriate empty and loading states.

Do not duplicate the board implementation just to create a separate sprint board. Extract shared board components and logic where useful.

### Phase 2 acceptance criteria

1. Backlog issues can be reordered.
2. Users can create and manage sprints.
3. Issues can move between backlog and sprint.
4. Sprint state transitions follow business rules.
5. Active sprint issues appear on the correct board.
6. Sprint completion preserves history and handles unfinished issues explicitly.
7. Authorization and cross-project data integrity are enforced.
8. Migrations, tests, and documentation are complete.

---

# PHASE 3 — EPICS, ISSUE HIERARCHY, AND RELEASES

Implement after Phases 1 and 2 are stable.

## 14. Epics

Add an epic concept for grouping related issues.

Requirements:

- Create and edit epics.
- Assign issues to an epic.
- Filter issues by epic.
- Display epic progress.
- View issues belonging to an epic.
- Prevent invalid cross-project or cross-workspace associations.

Determine whether an epic should be represented by a separate entity or by a compatible extension of the current issue model. Prefer a separate entity if it leads to clearer semantics and fewer special cases.

Document the decision before implementing it.

## 15. Subtasks and Issue Links

Support issue hierarchies and relationships.

Possible capabilities:

- Parent issue and subtasks.
- Blocks.
- Is blocked by.
- Relates to.
- Depends on.

Requirements:

- Prevent self-referential links.
- Prevent invalid cycles in hierarchical relationships.
- Enforce the intended project and workspace boundaries.
- Display parent and child issues.
- Define deletion behavior.
- Preserve issue history.

Do not confuse issue hierarchy with issue dependency links. Model them separately if their rules differ.

## 16. Releases and Timeline

Add version or release management.

Possible fields:

- `id`
- `project_id`
- `name`
- `description`
- `start_date`
- `release_date`
- `state`

Allow issues to be associated with a release.

Build a timeline view showing epics, due dates, and releases.

Do not attempt to build a fully featured Gantt chart before basic data and relationships are stable.

---

# PHASE 4 — WORKFLOW CONFIGURATION, FILTERS, AND PERMISSIONS

## 17. Workflow Configuration

Allow authorized administrators to configure workflow behavior where practical.

Requirements:

- Define available statuses.
- Define allowed transitions.
- Validate transitions in the backend.
- Keep existing issues compatible with any migration.
- Provide safe defaults.
- Prevent invalid transitions.
- Preserve activity history.

Avoid implementing a generic workflow engine unless the project truly needs one. Start with a simple, well-defined workflow model.

## 18. Saved Filters

Allow users to save useful issue queries.

Support filters such as:

- Project.
- Assignee.
- Status.
- Priority.
- Issue type.
- Sprint.
- Epic.
- Labels, if supported.
- Due date.
- Text search.

Allow users to save personal filters and, optionally, share filters with authorized workspace members.

Validate filter definitions. Do not execute arbitrary SQL or accept unsafe query expressions from users.

## 19. Authorization and Bulk Operations

Review and strengthen permissions across all new functionality.

Implement clear rules for:

- Workspace owners.
- Workspace administrators.
- Members.
- Issue creators and assignees, where relevant.

Support bulk operations only when there is a clear user need, such as updating several issues' assignees, priorities, or statuses.

For every mutation:

- Verify workspace and project membership.
- Validate target resources.
- Enforce permission rules in the backend.
- Use transactions for related updates.
- Record important changes in activity history.
- Return predictable errors.

Frontend visibility rules are not a substitute for backend authorization.

---

# PHASE 5 — REPORTS AND DASHBOARDS

## 20. Scrum Reports

Build reports from real issue and sprint data.

Potential reports:

- Sprint burndown.
- Sprint completion report.
- Velocity across completed sprints.

Define the underlying metrics and data requirements before implementation.

For burndown, decide whether the metric is based on story points, issue counts, or another effort measure. If story points are not yet implemented, do not pretend issue counts are story points.

Historical reports must not rely only on current issue values if the report requires historical status or scope information. Introduce the necessary snapshots or activity records where justified.

## 21. Kanban Reports

Potential reports:

- Cumulative flow diagram.
- Cycle time.
- Lead time.
- Work-in-progress trends.

Define when the relevant clocks start and stop.

Ensure status-transition history is reliable before calculating cycle time or cumulative flow.

## 22. Dashboards

Allow users to combine selected reports and summary widgets.

Requirements:

- Clear data source for every widget.
- Appropriate access control.
- Empty states for projects without enough historical data.
- Reasonable query performance.
- Accurate calculations and documented assumptions.

Do not invent metrics or present misleading charts when the underlying data is insufficient.

---

# PHASE 6 — AUTOMATION AND INTEGRATIONS

## 23. Automation Rules

Start with simple event-driven rules.

Possible triggers:

- Issue status changes.
- Issue is assigned.
- Issue approaches its due date.
- Sprint starts.
- Sprint completes.

Possible actions:

- Send a notification.
- Change an assignee.
- Change status when permitted.
- Create an activity record.

Requirements:

- Rules must be authorized and validated.
- Prevent infinite automation loops.
- Define execution order and failure behavior.
- Record rule executions.
- Avoid blocking HTTP requests on long-running automation.
- Keep the initial implementation simple.

Do not introduce a message broker unless there is a demonstrated need.

## 24. Integrations

Only after core features are reliable, consider:

- GitHub integration.
- Webhooks.
- Email notifications.
- External development workflow links.

Use secure secrets management, signature verification for incoming webhooks where appropriate, retry policies, and permission checks.

---

# 25. Database and Migration Requirements

Before each database change:

1. Inspect the current schema and migration history.
2. Identify existing data that could be affected.
3. Design a backward-compatible migration when practical.
4. Add constraints and indexes based on actual access patterns.
5. Plan backfills for existing records.
6. Verify the migration against a representative database.
7. Add tests for the new relationships and constraints.

Use Flyway migrations as the schema source of truth.

Do not modify a migration that has already been applied to shared or deployed environments. Create a new migration instead.

Do not use Hibernate automatic schema updates in production.

Preserve existing UUIDs and data whenever possible.

Document migration risks and rollback limitations, particularly for destructive schema changes.

# 26. Backend Architecture Requirements

Follow the existing architecture where reasonable.

Use clear separation between:

- Controllers.
- Services.
- Repositories.
- Entities.
- Request and response DTOs.
- Validation.
- Authorization.
- Error handling.

Keep business logic in services or appropriate domain components rather than controllers.

Use transactions for operations that must succeed or fail together.

Avoid exposing JPA entities directly as public API responses.

Use pagination for potentially large result sets.

Avoid N+1 queries when loading board cards and sprint issues.

Ensure rank updates, issue transitions, sprint operations, and key generation are concurrency-safe to the extent required by the design.

# 27. Frontend Requirements

Preserve the current React + TypeScript architecture and styling system.

Build reusable components where appropriate:

- `IssueCard`
- `KanbanBoard`
- `KanbanColumn`
- `BoardToolbar`
- `IssueFilters`
- `IssueDetail`
- `BacklogView`
- `SprintPanel`
- `SprintSelector`
- `SprintBoard`
- `EpicPanel`
- `ReleaseTimeline`

These are suggested names. Reuse existing components when they already serve the same purpose.

Requirements:

- Use typed API request and response models.
- Centralize API calls.
- Handle loading, error, and empty states.
- Keep frontend state consistent with server responses.
- Preserve search and filter state when appropriate.
- Ensure drag-and-drop works with keyboard-accessible alternatives where practical.
- Keep the interface responsive.
- Avoid giant components.
- Avoid duplicating board logic across Kanban and Scrum views.
- Do not use mock data as if it were persisted server data.

# 28. Testing Requirements

For each phase, add tests for the new functionality.

### Backend

Use JUnit 5, Mockito, and Spring Boot Test.

Test:

- Issue key generation and uniqueness.
- Ranking and reorder operations.
- Status changes.
- Pagination and filters.
- Permission enforcement.
- Cross-workspace and cross-project access.
- Sprint lifecycle transitions.
- Invalid sprint and issue associations.
- Transactional behavior.
- Migration compatibility where practical.

Use integration tests against PostgreSQL or Testcontainers where appropriate.

### Frontend

Test:

- Correct board column rendering.
- Drag-and-drop success and failure.
- Filter composition.
- Issue ordering.
- Backlog and sprint transitions.
- Loading, error, and empty states.
- Unauthorized action handling.
- Routing and navigation.

### Regression testing

Ensure existing workspace, project, issue, comment, activity, notification, and membership functionality continues to work.

Do not remove or weaken existing tests merely to make the new implementation pass.

# 29. Definition of Done

A feature is complete only when:

1. The implementation fits the existing architecture.
2. Database changes have appropriate migrations.
3. Backend validation and authorization are implemented.
4. Frontend behavior is connected to the real API.
5. Errors are handled clearly.
6. Relevant automated tests pass.
7. Existing functionality has not regressed.
8. Documentation is updated.
9. The feature has been manually verified where possible.
10. Any known limitations are explicitly documented.

# 30. Required Project Documentation

Maintain the following documentation as the project evolves:

- Root README.
- Architecture overview.
- ERD and schema notes.
- API documentation.
- Kanban and ranking design.
- Issue-key generation design.
- Sprint lifecycle rules.
- Authorization rules.
- Setup and migration instructions.
- Testing instructions.
- Known limitations.
- Future roadmap.

Record major architectural decisions and explain why a particular design was chosen.

# 31. Implementation Order

Follow this priority order:

1. Repository inspection and current-state assessment.
2. Issue keys.
3. Persistent issue ranking.
4. Kanban board and drag-and-drop.
5. Quick filters and assignee swimlanes.
6. Workspace/project issue APIs and pagination.
7. Backlog.
8. Sprint data model and lifecycle.
9. Active sprint board.
10. Epics.
11. Subtasks, issue links, and releases.
12. Workflow customization and saved filters.
13. Reports and dashboards.
14. Automation and external integrations.

Do not start Phase 2 until Phase 1's acceptance criteria are satisfied, unless I explicitly authorize parallel work.

Do not begin advanced reporting before the underlying issue history and sprint data are reliable.

# 32. How You Must Work

For every milestone:

1. Explain the goal and scope.
2. Identify the files to inspect or change.
3. Present the implementation plan.
4. Explain important design decisions.
5. Implement a small, coherent set of changes.
6. Provide commands to run.
7. Run or guide me through the relevant tests.
8. Review the results and fix issues.
9. Update the roadmap and documentation.
10. Proceed to the next milestone only after verification.

If you discover that an assumption in this prompt conflicts with the existing implementation, explain the conflict and recommend the smallest safe adjustment.

Do not overwrite files without understanding their current responsibilities.

Do not claim that you have run commands or tests when you have not.

If you have access to repository-editing and terminal tools, use them to inspect, implement, and verify changes directly. Otherwise, provide exact file paths, complete code or patches, and verification commands.

# 33. Start Now

Begin by inspecting the existing Chila repository.

Focus on:

- The issue entity, service, repository, controller, and DTOs.
- The existing database migrations.
- The current project model and project key configuration.
- The current issue status and priority model.
- `frontend/src/features/issues/IssuesPanel.tsx`.
- `frontend/src/auth/api.ts`.
- The existing frontend router and layout.
- Existing tests.

Then provide:

1. A concise current-state assessment.
2. A list of files that need modification.
3. The proposed issue-key generation strategy.
4. The proposed ranking strategy.
5. The proposed API changes.
6. The database migration plan.
7. The Phase 1 implementation checklist.

After presenting the assessment, begin implementing **Phase 1: Issue Keys, Persistent Ranking, and Kanban Board**, unless a critical missing decision prevents safe implementation.

The final objective is to make Chila a reliable, maintainable Jira-like application with working Kanban and Scrum workflows, rather than merely adding a large number of disconnected features.
