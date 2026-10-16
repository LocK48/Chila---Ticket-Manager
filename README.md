# Chila

Chila is a team project and issue management platform. This repository is being built incrementally as a Java full-stack portfolio project.

## Current milestone: Kanban and Scrum workflow foundation

- Java 21 and Spring Boot 3.5 backend.
- React, TypeScript, and Vite frontend.
- `GET /api/v1/health` reports backend status.
- Vite proxies `/api` requests to the backend on `localhost:8080`.
- CORS allows the local Vite origin and can be changed with `CHILA_CORS_ALLOWED_ORIGIN`.
- PostgreSQL connects through Spring Data JPA; Flyway applies SQL migrations and Hibernate validates the schema.
- Registration and login use BCrypt password hashes and short-lived HttpOnly JWT cookies with CSRF protection.
- Protected profile pages support current-user retrieval, display-name updates, and logout.
- Workspaces can be created, listed, viewed, updated, and deleted through authenticated APIs.
- Workspace creation grants the creator the `OWNER` role. Members can be added by registered email and assigned `ADMIN` or `MEMBER` roles.
- Workspace membership is enforced on every workspace endpoint. Non-members receive `404` for workspace-specific resources.
- The signed-in home page can create and switch workspaces, edit workspace details, add existing users, change member roles, remove members or leave, and delete a workspace as its owner.
- Projects can be created and listed inside workspaces; owners and admins can update details and archive or restore them.
- Issues have sequential project-scoped keys, persisted column ranking, and a workspace-protected key lookup API. Project boards support status columns, issue creation, quick filters, drag-and-drop, and keyboard-based ordering.
- Product backlogs keep their own order and issues can be planned into project sprints. Future, active, and closed sprints have guarded lifecycle transitions; the active sprint board reuses Kanban and completion routes unfinished issues explicitly. See [docs/phase-2-design.md](docs/phase-2-design.md).

Workspace and membership API/schema decisions are documented in [docs/phase-c-design.md](docs/phase-c-design.md). Phase D project decisions are in [docs/phase-d-design.md](docs/phase-d-design.md). Kanban and Scrum data/API decisions are in [docs/issue-keys-ranking-kanban.md](docs/issue-keys-ranking-kanban.md) and [docs/phase-2-design.md](docs/phase-2-design.md). Later hierarchy, reporting, CI, and deployment work remains planned.

## Prerequisites

- Java 21
- Node.js and npm
- Docker Desktop (or another Docker Engine with Compose)

## Run locally

From the repository root, start PostgreSQL:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up -d postgres
docker compose ps
```

The sample `.env` values are for local development only. Before starting the backend, replace `CHILA_JWT_SECRET` in `.env` with a private Base64-encoded secret of at least 32 bytes (for example, generate one with `openssl rand -base64 32`). Never commit `.env` or use a development key in production. The backend imports the root `.env` file when run from `backend`; Compose reads the same database settings. Flyway applies migrations automatically when the backend starts.

If another PostgreSQL server already uses port `5432`, set `POSTGRES_PORT=5433` in `.env` so the Compose database and backend use port `5433` without changing the other server.

To use Neon instead of local Compose storage, set `POSTGRESQL_DB_URL` in `.env` to the Neon PostgreSQL connection URL. The backend accepts Neon’s `postgresql://...` URL or a `jdbc:postgresql://...` URL; credentials embedded in the URL are used unless `SPRING_DATASOURCE_USERNAME` or `SPRING_DATASOURCE_PASSWORD` overrides them. `SPRING_DATASOURCE_URL`, when set, has highest priority. Keep the URL private and restart the backend after changing it. The local Compose database remains available as the fallback when `POSTGRESQL_DB_URL` is unset.

In one terminal, start the backend:

```powershell
cd backend
./mvnw.cmd spring-boot:run
```

Check `http://localhost:8080/api/v1/health`. A successful response looks like:

```json
{"status":"UP","application":"Chila Backend"}
```

To register or log in through the API, first call `GET /api/v1/auth/csrf` and keep the returned `XSRF-TOKEN` cookie. Send it and its value in the `X-XSRF-TOKEN` header on `POST /api/v1/auth/register` or `POST /api/v1/auth/login`. Login returns a safe user object and sets a short-lived, HttpOnly access cookie. Re-fetch the CSRF cookie after login and logout before another state-changing request. Use `GET /api/v1/users/me` to check the session, `PATCH /api/v1/users/me` to update the display name, and `POST /api/v1/auth/logout` to clear the browser cookie. Logout does not revoke a copied JWT; it expires after 15 minutes. The full contract is in [docs/phase-b-design.md](docs/phase-b-design.md).

In another terminal, start the frontend:

```powershell
cd frontend
npm ci
npm run dev
```

Open the local URL printed by Vite. The page should show the backend connection as online.

If port `8080` is already in use, start the backend with `SERVER_PORT=8081` and set `CHILA_API_TARGET=http://localhost:8081` in the frontend terminal before `npm run dev`.

To stop PostgreSQL while keeping its data volume, run `docker compose down` from the repository root.

## Verify the foundation

```powershell
cd backend
./mvnw.cmd test
```

```powershell
cd frontend
npm run build
```

## Issue keys and Kanban

Issues retain UUIDs and now also receive stable project-scoped keys such as `ISS-42`. The project board supports persistent ordering, status columns, combined quick filters, issue-key search, and keyboard-accessible moves. V6 backfills keys and ranks for existing issues. See [docs/issue-keys-ranking-kanban.md](docs/issue-keys-ranking-kanban.md) for API, ranking, migration, and limitation details.

Run `./mvnw.cmd test` from `backend` (Docker is required for the PostgreSQL Testcontainers suite), then run `npm run build` and `npm run lint` from `frontend`. Open a project's Board tab in the browser to manually check pointer and keyboard moves.

## Roadmap

1. Foundation: local backend/frontend startup, health check, CORS, and PostgreSQL service. (Complete.)
2. Phase B: PostgreSQL/Flyway foundation, authentication, and user management. (Complete.)
3. Phase C: workspaces and membership (complete; backend APIs, schema, frontend workflows, and access checks verified).
4. Phase D: projects (complete; schema, membership-protected APIs, frontend project workflows, and archive/restore implemented).
5. Phase E: issues and task management (complete; scoped APIs, assignee validation, filtering/search/pagination, archive/restore, frontend workflows, and integration tests).
6. Phase F: collaboration and auditability (complete; issue comments, field activity history, assignment/comment notifications, membership-scoped notification access, and status transition rules).
7. Phase G: frontend user experience (complete; responsive workspace/project/issue flows, issue details, collaboration, notifications, and feedback states; see [docs/phase-g-ux.md](docs/phase-g-ux.md)).
8. Jira workflow foundation: issue keys, persistent ranking, and Kanban board (implemented; backend tests pass, browser interaction verification remains).
9. Phase 2: backlog and Scrum sprint lifecycle (implemented; PostgreSQL integration rerun and browser verification remain).
10. Epics, subtasks, issue links, releases, workflow customization, saved filters, reports, automation, CI, and deployment.

Each milestone will be verified before moving on.
