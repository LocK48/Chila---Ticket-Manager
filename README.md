# Chila

Chila is a team project and issue management platform. This repository is being built incrementally as a Java full-stack portfolio project.

## Current milestone: local foundation

- Java 21 and Spring Boot 3.5 backend.
- React, TypeScript, and Vite frontend.
- `GET /api/v1/health` reports backend status.
- Vite proxies `/api` requests to the backend on `localhost:8080`.
- The frontend displays whether the backend health endpoint responds.

Authentication, PostgreSQL, workspaces, projects, issues, and CI are planned; they are not implemented yet.

## Prerequisites

- Java 21
- Node.js and npm

## Run locally

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

1. Foundation: local backend/frontend startup and health check.
2. Database design: ERD, constraints, and Flyway migration review.
3. Authentication and workspace membership.
4. Projects and issue workflows.
5. Frontend product workflows, tests, CI, and deployment.

Each milestone will be verified before moving on.
