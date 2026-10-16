# Chila Ticket Manager

Chila is a full-stack project and issue management application for teams. It brings workspace membership, project planning, Kanban and Scrum workflows, collaboration, reporting, and lightweight automation into one place. This is an evolving portfolio project inspired by common Jira workflows; it is not a Jira-compatible replacement.

## Contents

- [Features](#features)
- [Technology](#technology)
- [Architecture](#architecture)
- [Requirements](#requirements)
- [Local setup](#local-setup)
- [Configuration](#configuration)
- [Application routes](#application-routes)
- [API overview](#api-overview)
- [Database and migrations](#database-and-migrations)
- [Verification](#verification)
- [Project layout](#project-layout)
- [Current limitations](#current-limitations)
- [Design documents](#design-documents)

## Features

### Accounts and security

- Email and password registration with a six-digit email OTP.
- Password changes require a six-digit OTP sent to the signed-in account's email.
- OTPs expire after 10 minutes, allow up to five attempts, and have a 60-second resend delay. Codes are stored as password hashes.
- BCrypt password hashing, short-lived JWT access cookies (`HttpOnly`, 15 minutes), and CSRF protection for state-changing requests.
- Profile display-name updates, account security controls, and logout.

### Workspaces, projects, and access

- Create and switch between workspaces; edit or delete a workspace.
- Workspace roles: `OWNER`, `ADMIN`, and `MEMBER`. A workspace creator becomes its owner.
- Add existing Chila users to a workspace, change eligible roles, remove members, or leave a workspace.
- Create and edit projects; archive and restore projects without deleting issue history.
- Authorization is enforced by the backend. Workspace and project data is scoped to membership and role.

### Issue management and planning

- Create, edit, search, filter, sort, paginate, archive, and restore issues.
- Issue types: `TASK`, `BUG`, `STORY`; statuses: `TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`; priorities: `LOW`, `MEDIUM`, `HIGH`, `URGENT`.
- Stable project-scoped issue keys such as `APP-42`, sequential allocation, and persisted ordering within status columns.
- Kanban board with pointer drag-and-drop, keyboard moves, combined filters, and per-column pagination.
- Product backlog with independent ordering and sprint planning.
- Sprint lifecycle: future, active, and closed. Completing a sprint retains completed work in history and explicitly routes unfinished issues to the backlog or another future sprint.
- Epics with issue progress, parent/child issue hierarchy, directed issue links, releases, and due-date planning.
- Configurable project workflow transitions and private or workspace-shared saved issue filters.

### Collaboration and visibility

- Issue comments, append-only activity history, and in-app notifications for assignments and comments.
- Project dashboards with issue flow metrics, and sprint reports with completion snapshots, burndown, and velocity.
- Project automation rules can create a notification when an issue enters a selected status. Execution history is retained.
- Responsive frontend navigation, profile and workspace settings, and shared in-app confirmation dialogs for destructive actions.

## Technology

| Area | Technologies |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5, Spring Web, Spring Security, Spring Data JPA, Bean Validation, Spring Mail, Actuator |
| Frontend | React 19, TypeScript 6, React Router 7, Vite 8 |
| Database | PostgreSQL 17, Flyway SQL migrations |
| Backend integration tests | JUnit/Spring Test, PostgreSQL Testcontainers, Docker |

## Architecture

- The frontend is a React single-page application. During development, Vite proxies `/api` to the backend.
- The backend exposes versioned REST endpoints under `/api/v1` and is the authority for access checks and business rules.
- PostgreSQL is accessed through Spring Data JPA. Flyway owns schema changes; Hibernate validates the schema at startup and does not create or update it.
- Authentication uses an HttpOnly JWT cookie. The frontend sends a separate CSRF cookie value in the `X-XSRF-TOKEN` header for state-changing requests.
- Email is sent through configurable SMTP. The sender defaults to `SPRING_MAIL_USERNAME` when `CHILA_EMAIL_FROM` is empty.

## Requirements

- Java 21
- Node.js compatible with Vite 8 (Node.js 20.19+ or 22.12+)
- npm
- Docker Desktop or another Docker Engine with Compose for local PostgreSQL and the backend integration test suite
- An SMTP account for signup verification and password-change codes

## Local setup

Run the following from the repository root in PowerShell.

1. Create the backend environment file and a private JWT signing secret:

   ```powershell
   Copy-Item backend/.env.example backend/.env
   ```

   Edit `backend/.env`. Replace `CHILA_JWT_SECRET` with a private Base64-encoded secret of at least 32 bytes. Configure the SMTP variables described below. Never commit `.env` files or use example credentials in production.

2. Start the local PostgreSQL service:

   ```powershell
   docker compose up -d postgres
   docker compose ps
   ```

   Compose defaults to PostgreSQL 17 on port `5432`, database `chila`, and the development-only sample credentials. If port `5432` is already in use, set `POSTGRES_PORT=5433` in a root-level `.env` file for Compose and set the same port in `backend/.env` for the backend.

3. Start the backend in a terminal:

   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```

   By default, the backend listens on port `8080`. Flyway applies outstanding migrations at startup. Check `http://localhost:8080/api/v1/health`; a healthy response is:

   ```json
   {"status":"UP","application":"Chila Backend"}
   ```

4. Start the frontend in another terminal:

   ```powershell
   cd frontend
   npm ci
   npm run dev
   ```

   Open the local URL shown by Vite (usually `http://localhost:5173`).

To run the backend from the repository root instead, use `backend/.env` as described above; the application configuration searches both the current directory and `backend/.env`.

### Use Neon or another PostgreSQL service

Set `POSTGRESQL_DB_URL` in `backend/.env` to a PostgreSQL or JDBC PostgreSQL URL. `SPRING_DATASOURCE_URL` takes precedence if it is set. Credentials in the URL are used unless `SPRING_DATASOURCE_USERNAME` or `SPRING_DATASOURCE_PASSWORD` overrides them. Keep database URLs private. Local Compose PostgreSQL remains the default when no cloud URL is set.

### Use a different backend port

Start the backend with `SERVER_PORT=8081` (PowerShell: `$env:SERVER_PORT="8081"`) and set `CHILA_API_TARGET=http://localhost:8081` before starting Vite. For a separately hosted API, configure `VITE_API_BASE_URL` in the frontend environment and set `CHILA_CORS_ALLOWED_ORIGIN` on the backend to the exact frontend origin.

## Configuration

The checked-in template is [backend/.env.example](backend/.env.example). The real `backend/.env` is local-only and ignored by Git.

| Variable | Purpose |
| --- | --- |
| `CHILA_JWT_SECRET` | Private Base64 signing key; use at least 32 bytes of key material. Required. |
| `CHILA_JWT_ISSUER` | JWT issuer; defaults to `chila-local`. |
| `CHILA_AUTH_COOKIE_SECURE` | Enable `Secure` cookies in HTTPS deployments; local HTTP defaults to `false`. |
| `CHILA_CORS_ALLOWED_ORIGIN` | Exact browser origin allowed by the backend; defaults to `http://localhost:5173`. |
| `POSTGRESQL_DB_URL` | Optional Neon/PostgreSQL connection URL. |
| `SPRING_DATASOURCE_URL` | Explicit JDBC URL; overrides `POSTGRESQL_DB_URL`. |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Optional explicit database credential overrides. |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT` | Local Compose database settings and backend fallback connection values. |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` | SMTP host and port (port defaults to `587`). |
| `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | SMTP authentication credentials. |
| `SPRING_MAIL_SMTP_AUTH`, `SPRING_MAIL_STARTTLS` | SMTP auth and STARTTLS switches; both default to `true`. |
| `CHILA_EMAIL_FROM` | Sender address. When empty, Chila uses `SPRING_MAIL_USERNAME`. |
| `SERVER_PORT` | Backend HTTP port; defaults to `8080`. |
| `CHILA_API_TARGET` | Vite development proxy target; defaults to `http://localhost:8080`. |
| `VITE_API_BASE_URL` | Optional frontend API base URL for a separately hosted backend. |

SMTP must be configured for real signup and password-change emails. Without a working mail server, OTP requests return an email-delivery error and those account actions cannot complete.

## Application routes

| Route | Page |
| --- | --- |
| `/login`, `/register` | Sign in and OTP-verified account creation |
| `/workspaces/:workspaceId/overview` | Workspace overview and recent work |
| `/workspaces/:workspaceId/projects` | Project directory |
| `/workspaces/:workspaceId/projects/:projectId` | Project issues, board, backlog, sprint, epics, releases, reports, and settings tabs |
| `/workspaces/:workspaceId/issues` | Issues across visible workspace projects |
| `/workspaces/:workspaceId/my-issues` | Issues assigned to the current user |
| `/workspaces/:workspaceId/issues/:issueReference` | Issue detail, discussion, activity, and relationships |
| `/workspaces/:workspaceId/members` | Workspace membership and roles |
| `/workspaces/:workspaceId/settings` | Workspace and workflow settings |
| `/workspaces/:workspaceId/profile`, `/profile` | Account profile and password security |

Workspace issue pages combine project-scoped results in the frontend; they are not a single server-paginated workspace issue query. The backend still enforces authorization regardless of what the UI displays.

## API overview

All API routes use `/api/v1`. The primary resource groups are:

| Area | Representative routes |
| --- | --- |
| Health and authentication | `GET /health`, `GET /auth/csrf`, `POST /auth/registration-otp`, `/auth/register`, `/auth/login`, `/auth/logout` |
| Users and notifications | `/users/me`, `/users/me/password-otp`, `/users/me/password`, `/users/me/notifications`, `/notifications/{id}/read` |
| Workspaces and members | `/workspaces`, `/workspaces/{workspaceId}/members` |
| Projects and issues | `/workspaces/{workspaceId}/projects`, `/projects/{projectId}/issues`, `/issues/{issueId}` |
| Planning | `/projects/{projectId}/backlog`, `/projects/{projectId}/sprints`, `/projects/{projectId}/epics`, `/projects/{projectId}/releases` |
| Workflow and filters | `/projects/{projectId}/workflow`, `/projects/{projectId}/filters` |
| Collaboration and reports | `/issues/{issueId}/comments`, `/issues/{issueId}/activities`, `/projects/{projectId}/reports` |
| Automation | `/projects/{projectId}/automation/rules`, `/projects/{projectId}/automation/executions` |

State-changing requests require a valid CSRF token. First call `GET /api/v1/auth/csrf`, retain the `XSRF-TOKEN` cookie, and send its value as `X-XSRF-TOKEN`. Signup requires requesting a code from `/api/v1/auth/registration-otp` and sending `verificationCode` with `/api/v1/auth/register`. A signed-in user requests a password OTP from `/api/v1/users/me/password-otp` and submits `verificationCode` and `newPassword` to `/api/v1/users/me/password`.

## Database and migrations

PostgreSQL is the supported database. Flyway migrations `V1`–`V12` create and extend the user, workspace, project, issue, collaboration, issue-key/ranking, sprint, hierarchy/release, workflow/filter, reporting, automation, and email-verification schemas. Migration files live in `backend/src/main/resources/db/migration`. Hibernate runs with `ddl-auto: validate`; do not edit an applied migration to change a deployed schema—add a new migration instead.

## Verification

Run the backend suite from `backend` (Docker is required for PostgreSQL Testcontainers):

```powershell
.\mvnw.cmd test
```

Run frontend static checks from `frontend`:

```powershell
npm run lint
npm run build
```

The frontend currently has no dedicated automated React component-test runner. Important interactions such as drag-and-drop should also be checked in a browser.

## Project layout

```text
backend/       Spring Boot REST API, domain services, migrations, and integration tests
frontend/      React, TypeScript, and Vite application
docs/          Phase designs, architecture decisions, and localized documentation
docker-compose.yml  Local PostgreSQL service
README.md      Project overview and English setup guide
```

## Current limitations

- This project implements selected project-management workflows; it does not provide full Jira feature or API compatibility.
- Workspace issue views aggregate project-scoped API results in the frontend and do not provide true workspace-wide server pagination.
- Planning selectors and some overview lists use bounded result sets. Epic progress reflects current issue status; links do not automatically block workflow transitions. Release dates are planning metadata rather than a Gantt schedule.
- Issues do not have story points or labels. Workflow configuration changes allowed transitions, not the status vocabulary. Saved filters do not include labels.
- Automation currently supports status-triggered notifications to the reporter or assignee. Scheduled rules, outbound webhooks, and broader integration actions are not implemented.
- Authentication has no refresh token or server-side JWT revocation. Logout clears the browser cookie; a copied token remains valid until its short expiry.

## Design documents

- [Phase B: database and authentication](docs/phase-b-design.md)
- [Phase C: workspaces and membership](docs/phase-c-design.md)
- [Phase D: projects](docs/phase-d-design.md)
- [Phase E: issues and task management](docs/phase-e-design.md)
- [Phase F: collaboration and auditability](docs/phase-f-design.md)
- [Phase G: frontend experience](docs/phase-g-ux.md)
- [Issue keys, ranking, and Kanban](docs/issue-keys-ranking-kanban.md)
- [Phase 2: backlog and Scrum sprints](docs/phase-2-design.md)
- [Phase 3: epics, hierarchy, and releases](docs/phase-3-design.md)
- [Phase 4: workflow and saved filters](docs/phase-4-design.md)
- [Phase 5: reports and dashboards](docs/phase-5-design.md)
- [Phase 6: project automation](docs/phase-6-design.md)
- [Vietnamese project guide](docs/README.vi.md)
