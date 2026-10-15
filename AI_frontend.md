# PROMPT: DESIGN AND IMPLEMENT CHILA'S MULTI-PAGE FRONTEND WITH SIDEBAR NAVIGATION

## 1. Your Role

Act as a Senior Frontend Engineer and UI/UX Designer experienced in building modern SaaS applications using React and TypeScript.

Help me design and implement the frontend for **Chila**, a team project and issue management platform inspired by Jira and Linear.

The objective is to create a professional, maintainable, responsive, multi-page application with a persistent sidebar, clear navigation, reusable components, and a consistent visual design.

Do not rebuild the entire application blindly. First inspect the existing project, then implement the UI incrementally while preserving working functionality.

## 2. Existing Technology Stack

Frontend:

- React
- TypeScript
- Vite
- React Router
- Existing API client, if already implemented

Backend:

- Java 21
- Spring Boot
- Spring Security
- REST API
- PostgreSQL

The frontend communicates with the backend through REST APIs.

Do not replace the backend or introduce another backend framework.

If a backend endpoint has not been implemented, use a clearly identified mock data layer only for UI development. Do not pretend that mock operations persist to PostgreSQL.

## 3. Product and Design Direction

Chila is a project and issue management platform for small teams.

Design the interface as a modern productivity SaaS application with visual inspiration from Linear, Jira, and Notion, without copying their branding.

Visual direction:

- Clean, minimal, professional.
- Neutral background with subtle borders.
- Clear typography and visual hierarchy.
- One consistent accent color.
- Consistent spacing and component sizes.
- Compact but readable tables and issue lists.
- Subtle hover, active, and focus states.
- Responsive behavior for desktop, tablet, and mobile.
- Accessible contrast and keyboard navigation.

Avoid:

- Excessive gradients and decorative effects.
- Too many cards.
- Oversized headings.
- Unnecessary animations.
- Inconsistent colors and spacing.
- Putting the entire application into one giant component.

Use a consistent design system with reusable colors, spacing, typography, buttons, inputs, dialogs, badges, tables, and navigation components.

Choose one styling approach that fits the existing repository, such as Tailwind CSS or CSS Modules. Do not introduce multiple competing styling systems.

## 4. Application Layout

Create a shared application layout consisting of three main regions:

1. Persistent left sidebar.
2. Main content area.
3. Optional page-level header and contextual actions.

The sidebar should remain consistent while navigating between application pages.

Suggested desktop layout:

```text
┌──────────────────────────────────────────────────────────────┐
│ Chila                                                       │
├────────────────┬─────────────────────────────────────────────┤
│ WORKSPACE      │ Page title                  Primary action   │
│ Workspace name │ Breadcrumbs / page context                  │
│                ├─────────────────────────────────────────────┤
│ Overview       │                                             │
│ Projects       │                                             │
│ Issues         │              Page content                   │
│ Members        │                                             │
│                │                                             │
│ SETTINGS       │                                             │
│ Workspace      │                                             │
│ settings       │                                             │
│                │                                             │
│ User profile   │                                             │
└────────────────┴─────────────────────────────────────────────┘
```

The layout must adapt to screen size.

### Desktop

- Sidebar approximately 240–260px wide.
- Sidebar remains visible while page content changes.
- Main content occupies the remaining space.
- Long tables and issue lists must not break the layout.
- Use consistent page padding and content width.

### Tablet

- Reduce sidebar width or allow it to collapse.
- Keep navigation accessible.
- Prevent horizontal overflow where possible.

### Mobile

- Replace the persistent sidebar with a drawer or slide-out navigation panel.
- Provide a visible menu button.
- Close the navigation drawer after selecting a destination.
- Ensure forms and issue details remain usable on small screens.

## 5. Sidebar Navigation

Implement a reusable `AppSidebar` component.

The sidebar should contain the following sections.

### A. Brand

- Chila logo or a simple text-based brand mark.
- Application name.
- Optional collapse button on desktop.

### B. Workspace Selector

Show the currently selected workspace.

Requirements:

- Display the workspace name.
- Allow switching between available workspaces.
- Provide a workspace creation action.
- Reflect the selected workspace in relevant page URLs or application state.
- Do not allow users to access workspaces they are not authorized to use.

### C. Main Navigation

Create these navigation items:

1. **Overview**
   - Route: `/workspaces/:workspaceId/overview`
   - Display a summary of workspace activity, projects, assigned issues, and progress.

2. **Projects**
   - Route: `/workspaces/:workspaceId/projects`
   - List projects belonging to the current workspace.

3. **Issues**
   - Route: `/workspaces/:workspaceId/issues`
   - Provide a consolidated view of issues across accessible projects in the workspace.

4. **My Issues**
   - Route: `/workspaces/:workspaceId/my-issues`
   - Display issues assigned to the current user.

5. **Members**
   - Route: `/workspaces/:workspaceId/members`
   - Display workspace members and their roles.

### D. Workspace Settings

Add a separate navigation section:

- Workspace settings:
  `/workspaces/:workspaceId/settings`

Only show administrative actions to users with appropriate permissions.

### E. User Profile

At the bottom of the sidebar, display:

- User avatar or initials.
- User name.
- User email, where appropriate.
- A profile menu.

Profile menu actions:

- My profile.
- Account settings, if implemented.
- Log out.

Do not expose sensitive account information.

### F. Navigation Behavior

- Use React Router for navigation.
- Highlight the active navigation item.
- Support nested routes.
- Preserve the sidebar across route transitions.
- Avoid full-page reloads.
- Use appropriate icons from one icon library, such as Lucide.
- Provide accessible labels and tooltips where necessary.
- Show loading and error states when workspace data is unavailable.
- Do not hard-code a fake logged-in user in production code.

Use a route-aware layout rather than manually toggling page components based on button clicks.

## 6. Page Specifications

Implement each page as an independent component with a clear responsibility.

### Page 1: Overview

Route:
`/workspaces/:workspaceId/overview`

Include:

- Welcome heading.
- Workspace name.
- Total projects.
- Open issues.
- Issues assigned to the current user.
- Recently updated issues.
- Recent project activity, if supported by the API.
- Quick actions to create a project or issue, subject to permissions.

Do not invent analytics that cannot be calculated from available data. Use empty states when the workspace has no data.

### Page 2: Projects

Route:
`/workspaces/:workspaceId/projects`

Include:

- Page title and description.
- Search input.
- Optional status filters.
- Grid or table view.
- Project name and key.
- Project status.
- Issue count, if available.
- Created date or last updated date, if available.
- Create project button.
- Project detail navigation.

Project creation and editing should use a dialog or a dedicated route, depending on the complexity of the form.

### Page 3: Project Details

Route:
`/workspaces/:workspaceId/projects/:projectId`

Include:

- Project name and key.
- Project description.
- Project-level navigation or tabs.
- Issues belonging to the project.
- Issue creation action.
- Project settings, subject to permissions.

Suggested project tabs:

- Issues.
- Overview.
- Settings.

Keep the global sidebar visible. Project tabs should only affect the content area.

### Page 4: All Issues

Route:
`/workspaces/:workspaceId/issues`

Include:

- Search.
- Filters for status, priority, assignee, and project.
- Sort controls.
- Pagination.
- Issue table or list.
- Create issue button.

Suggested columns:

- Issue key or identifier.
- Title.
- Status.
- Priority.
- Assignee.
- Project.
- Due date.

Provide clear visual badges for status and priority.

Support loading, error, and empty states.

### Page 5: My Issues

Route:
`/workspaces/:workspaceId/my-issues`

Display issues assigned to the currently authenticated user.

Include:

- Status filters.
- Priority filters.
- Due-date information.
- Links to issue details.
- An informative empty state.

Use the authenticated user's identity rather than accepting an arbitrary user ID from the frontend as proof of identity.

### Page 6: Issue Details

Route:
`/workspaces/:workspaceId/issues/:issueId`

Include:

- Issue title.
- Description.
- Issue type.
- Status.
- Priority.
- Assignee.
- Reporter.
- Due date.
- Created and updated timestamps.
- Editable fields according to permissions.

Optional extensions:

- Comments.
- Activity history.
- Attachments.

Use a dedicated page or a responsive detail panel. Choose one approach and apply it consistently.

### Page 7: Members

Route:
`/workspaces/:workspaceId/members`

Include:

- Member list.
- Name and email.
- Workspace role.
- Join date, if available.
- Add or invite member action.
- Role management.
- Remove member action.

Hide unauthorized actions in the UI, but always enforce permissions in the backend.

If invitation APIs are not available, show a clearly marked placeholder or implement only the actions supported by the current backend.

### Page 8: Workspace Settings

Route:
`/workspaces/:workspaceId/settings`

Include:

- Workspace name and description.
- Update workspace information.
- Member and role management links.
- Workspace deletion only if supported and properly protected.

Require confirmation before destructive actions.

Do not allow members without sufficient permissions to edit administrative settings.

### Page 9: User Profile

Route:
`/profile`

Include:

- User name.
- Email.
- Profile information.
- Edit profile form if supported by the backend.
- Account-related information that is safe to display.

Use the authenticated user returned by the API.

### Page 10: Authentication

Routes:

- `/login`
- `/register`

Authentication pages should use a separate layout without the authenticated sidebar.

Include:

- Email and password fields.
- Registration name field.
- Validation.
- Loading state.
- Error messages.
- Successful login and logout behavior.
- Redirect to the intended page after login when appropriate.

Prevent unauthenticated users from accessing protected routes.

If a user is already authenticated, redirect them away from login or registration pages when appropriate.

## 7. Shared Components

Create reusable components rather than duplicating UI logic.

Suggested components:

- `AppLayout`
- `AppSidebar`
- `MobileNavigation`
- `WorkspaceSwitcher`
- `UserMenu`
- `PageHeader`
- `Breadcrumbs`
- `Button`
- `Input`
- `Select`
- `Dialog`
- `DropdownMenu`
- `StatusBadge`
- `PriorityBadge`
- `Avatar`
- `DataTable`
- `SearchInput`
- `FilterBar`
- `Pagination`
- `LoadingState`
- `ErrorState`
- `EmptyState`
- `ConfirmDialog`

Do not create all components in advance without a use case. Build shared components when they are needed by multiple pages.

## 8. Routing Architecture

Use React Router with nested routes and shared layouts.

The routing structure should conceptually resemble:

```text
/
├── login
├── register
├── profile
└── workspaces/:workspaceId
    ├── overview
    ├── projects
    │   └── :projectId
    ├── issues
    │   └── :issueId
    ├── my-issues
    ├── members
    └── settings
```

Adapt the route tree to avoid ambiguous routes and collisions between workspace issue lists and issue details.

Requirements:

- Public routes for authentication.
- Protected routes for authenticated pages.
- Shared `AppLayout` for workspace pages.
- Nested project and issue routes where appropriate.
- A not-found page.
- An unauthorized or access-denied state.
- Redirect behavior for missing workspace selection.
- Correct active sidebar states for nested pages.

Do not implement navigation using ad hoc conditionals in a single `App.tsx`.

## 9. Frontend Folder Structure

Inspect the existing structure first. Then adopt or adapt a maintainable organization such as:

```text
frontend/src/
├── api/
│   ├── client.ts
│   ├── auth.ts
│   ├── workspaces.ts
│   ├── projects.ts
│   └── issues.ts
├── app/
│   ├── router.tsx
│   └── providers.tsx
├── components/
│   ├── ui/
│   └── layout/
├── features/
│   ├── auth/
│   ├── overview/
│   ├── workspaces/
│   ├── projects/
│   ├── issues/
│   ├── members/
│   └── profile/
├── hooks/
├── layouts/
├── lib/
├── pages/
├── types/
├── App.tsx
└── main.tsx
```

Avoid duplicating files that already exist. Adapt the structure to the current repository and explain any significant changes.

## 10. API Integration

The backend is Java Spring Boot and PostgreSQL.

Use a centralized API client and typed API functions.

Requirements:

- Read the API base URL from an environment variable.
- Handle HTTP errors consistently.
- Handle authentication expiration.
- Avoid duplicating `fetch` logic across components.
- Define TypeScript request and response types.
- Show useful user-facing error messages.
- Keep backend authorization authoritative.
- Do not expose database credentials or backend secrets in frontend code.

Use existing endpoints whenever available.

Before implementing a page's API integration:

1. Inspect the existing backend endpoints.
2. Identify missing endpoints.
3. Separate real data from mock data.
4. Implement the UI against a clear API contract.

Do not invent API responses and silently assume the backend supports them.

## 11. UX Requirements

Every page must handle these states where relevant:

- Initial loading.
- Successful data loading.
- Empty results.
- API errors.
- Form validation errors.
- Permission restrictions.
- Saving or submitting.
- Success feedback.
- Destructive action confirmation.

Additional requirements:

- Sidebar navigation must work on every relevant route.
- The active item must reflect the current URL.
- Forms must be keyboard accessible.
- Dialogs must have appropriate focus behavior.
- Buttons must have meaningful labels.
- Tables must remain usable on smaller screens.
- Avoid unnecessary layout shifts.
- Preserve filters and search parameters in the URL where useful.
- Use realistic labels and content.
- Do not use placeholder buttons that appear functional but do nothing.

## 12. Implementation Plan

Work in the following stages.

### Stage 1 — Inspect and plan

- Inspect the current frontend files.
- Identify existing dependencies.
- Check the current routing and API integration.
- Propose the page structure and route tree.
- Identify missing components and dependencies.
- Present a concise implementation plan.

Do not modify files yet until the plan is clear.

### Stage 2 — Establish the application shell

- Configure React Router.
- Build `AppLayout`.
- Build the sidebar.
- Implement the workspace selector UI.
- Implement the user menu.
- Create page placeholders.
- Verify navigation and active states.
- Add responsive mobile navigation.

At the end of this stage, I should be able to navigate between all page routes even if some pages still use placeholder content.

### Stage 3 — Implement pages

Implement pages in this order:

1. Login and registration.
2. Workspace overview.
3. Projects list and project details.
4. All issues and issue details.
5. My Issues.
6. Members.
7. Workspace settings.
8. User profile.

Prioritize functional workflows over decorative polish.

### Stage 4 — Connect APIs

- Inspect backend endpoints.
- Implement typed API functions.
- Replace mock data with real API responses.
- Add loading, error, and empty states.
- Verify the end-to-end workflows.

### Stage 5 — Polish and test

- Verify all navigation paths.
- Test protected routes.
- Test responsive layouts.
- Test forms and validation.
- Test error handling.
- Remove unused components and dependencies.
- Fix TypeScript errors.
- Run the production build.

## 13. Definition of Done

The frontend is considered complete when:

- Each major feature has its own page or clearly defined route.
- A persistent sidebar provides navigation between workspace pages.
- The active navigation item is correct.
- Mobile navigation works.
- Authentication routes use a separate layout.
- Shared UI components are reused consistently.
- TypeScript has no unexplained errors.
- The frontend production build succeeds.
- Real API integration is used wherever the backend supports it.
- Mock data is clearly identified and does not masquerade as persistent data.
- Loading, error, empty, and permission states are handled.
- Backend authorization is not replaced by frontend-only checks.

## 14. How to Respond

Work incrementally.

For each stage:

1. Explain the goal.
2. List the files to create or update.
3. Provide complete code or clear patches.
4. Explain important design decisions.
5. Give commands to run.
6. State the expected result.
7. Wait for my confirmation before proceeding to the next stage.

Do not rewrite the entire project in one response.

Do not claim that code has been executed or tested unless it has actually been run.

If you cannot inspect my repository, ask me to provide the relevant files or directory tree.

## 15. Start Now

Start with **Stage 1: Inspect and plan**.

Ask me for my current `frontend/src` directory structure, `package.json`, and existing routing or API files if you cannot access the repository.

Then propose the sidebar design, route hierarchy, page structure, and implementation sequence tailored to the existing codebase.

The immediate goal is to establish a working multi-page Chila frontend with a reusable sidebar before implementing every business feature.
