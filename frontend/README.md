# Chila frontend

React, TypeScript, and Vite frontend for the Chila project and issue manager.

## Run locally

```sh
npm install
npm run dev
```

The Vite development server forwards `/api` requests to `http://localhost:8080` by default. Set `CHILA_API_TARGET` to change the proxy destination. For a separately hosted API, set `VITE_API_BASE_URL` in the frontend environment; leave it empty to use the proxy.

## Routes

- `/login`, `/register`
- `/workspaces/:workspaceId/overview`
- `/workspaces/:workspaceId/projects` and `/projects/:projectId` (nested under the workspace)
- `/workspaces/:workspaceId/issues`, `/my-issues`, and `/issues/:issueId`
- `/workspaces/:workspaceId/members` and `/settings`
- `/profile`

Issue list data comes from the existing project-scoped API. Workspace issue pages combine those project lists; they do not claim to provide workspace-wide server pagination. Permissions are reflected in the UI, while the backend remains authoritative.

## Checks

```sh
npm run lint
npm run build
```
