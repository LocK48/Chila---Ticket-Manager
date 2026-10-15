# Chila

Chila is a team project and issue management platform. This repository is being built incrementally as a Java full-stack portfolio project.

## Current milestone: local foundation

- Java 21 and Spring Boot 3.5 backend.
- React, TypeScript, and Vite frontend.
- `GET /api/v1/health` reports backend status.
- Vite proxies `/api` requests to the backend on `localhost:8080`.
- CORS allows the local Vite origin and can be changed with `CHILA_CORS_ALLOWED_ORIGIN`.
- Docker Compose provides a persistent local PostgreSQL database for the next milestone.
- The frontend displays whether the backend health endpoint responds.

Authentication, database integration and migrations, workspaces, projects, issues, and CI are planned; they are not implemented yet.

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

The sample `.env` values are for local development only. The application does not connect to PostgreSQL yet; that comes with the database-design milestone. Spring reads `CHILA_CORS_ALLOWED_ORIGIN` from the backend process environment; Compose reads database values from the root `.env` file.

In one terminal, start the backend:

```powershell
cd backend
./mvnw.cmd spring-boot:run
```

Check `http://localhost:8080/api/v1/health`. A successful response looks like:

```json
{"status":"UP","application":"Chila Backend"}
```

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

## Roadmap

1. Foundation: local backend/frontend startup, health check, CORS, and PostgreSQL service.
2. Database design: ERD, constraints, and Flyway migration review.
3. Authentication and workspace membership.
4. Projects and issue workflows.
5. Frontend product workflows, tests, CI, and deployment.

Each milestone will be verified before moving on.
