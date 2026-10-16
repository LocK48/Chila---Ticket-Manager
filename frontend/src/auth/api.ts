export type User = {
  id: string;
  email: string;
  displayName: string;
  createdAt: string;
};

export type MembershipRole = "OWNER" | "ADMIN" | "MEMBER";

export type Workspace = {
  id: string;
  name: string;
  description: string | null;
  role: MembershipRole;
  createdAt: string;
  updatedAt: string;
};

export type WorkspaceMember = {
  userId: string;
  email: string;
  displayName: string;
  role: MembershipRole;
  joinedAt: string;
};

export type ProjectStatus = "ACTIVE" | "ARCHIVED";

export type Project = {
  id: string;
  workspaceId: string;
  createdBy: string;
  name: string;
  key: string;
  description: string | null;
  status: ProjectStatus;
  createdAt: string;
  updatedAt: string;
};

export type IssueType = "TASK" | "BUG" | "STORY";
export type IssueStatus = "TODO" | "IN_PROGRESS" | "IN_REVIEW" | "DONE";
export type IssuePriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";
export type Issue = {
  id: string;
  projectId: string;
  sprintId: string | null;
  epicId: string | null;
  parentIssueId: string | null;
  releaseId: string | null;
  projectKey: string;
  issueKey: string;
  issueNumber: number;
  title: string;
  description: string | null;
  type: IssueType;
  status: IssueStatus;
  priority: IssuePriority;
  reporterId: string;
  reporterName: string;
  assigneeId: string | null;
  assigneeName: string | null;
  dueDate: string | null;
  archivedAt: string | null;
  createdAt: string;
  updatedAt: string;
};
export type IssuePage = {
  items: Issue[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};
export type Epic = {
  id: string;
  projectId: string;
  name: string;
  summary: string | null;
  color: string;
  totalIssues: number;
  completedIssues: number;
  createdAt: string;
  updatedAt: string;
};
export type EpicInput = { name: string; summary: string | null; color: string };
export type ReleaseState = "UNRELEASED" | "RELEASED" | "ARCHIVED";
export type ProjectRelease = {
  id: string;
  projectId: string;
  name: string;
  description: string | null;
  startDate: string | null;
  releaseDate: string | null;
  state: ReleaseState;
  issueCount: number;
  createdAt: string;
  updatedAt: string;
};
export type ReleaseInput = {
  name: string;
  description: string | null;
  startDate: string | null;
  releaseDate: string | null;
  state: ReleaseState | null;
};
export type IssueLinkType = "BLOCKS" | "DEPENDS_ON" | "RELATES_TO";
export type RelatedIssue = {
  id: string;
  issueKey: string;
  title: string;
  status: string;
};
export type IssueLink = {
  id: string;
  sourceIssueId: string;
  sourceIssueKey: string;
  sourceTitle: string;
  targetIssueId: string;
  targetIssueKey: string;
  targetTitle: string;
  type: IssueLinkType;
  createdAt: string;
};
export type IssueRelationships = {
  parent: RelatedIssue | null;
  children: RelatedIssue[];
  links: IssueLink[];
};
export type IssueComment = {
  id: string;
  issueId: string;
  authorId: string;
  authorName: string;
  body: string;
  createdAt: string;
};
export type IssueActivity = {
  id: string;
  issueId: string;
  actorId: string;
  actorName: string;
  action: string;
  fieldName: string | null;
  oldValue: string | null;
  newValue: string | null;
  createdAt: string;
};
export type Notification = {
  id: string;
  issueId: string | null;
  issueTitle: string | null;
  eventType: string;
  message: string;
  createdAt: string;
  readAt: string | null;
};
export type PageResult<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};
export type IssueFilters = {
  status?: IssueStatus;
  priority?: IssuePriority;
  type?: IssueType;
  assigneeId?: string;
  unassigned?: boolean;
  search?: string;
  includeArchived?: boolean;
  page?: number;
  size?: number;
  sort?: string;
  direction?: "asc" | "desc";
};
export type SprintState = "FUTURE" | "ACTIVE" | "CLOSED";
export type Sprint = {
  id: string;
  projectId: string;
  name: string;
  goal: string | null;
  state: SprintState;
  startDate: string | null;
  endDate: string | null;
  createdAt: string;
  updatedAt: string;
};
export type SprintInput = {
  name: string;
  goal: string | null;
  startDate: string | null;
  endDate: string | null;
};

export type RegistrationInput = {
  email: string;
  password: string;
  displayName: string;
};

export type ApiErrorBody = {
  code?: string;
  message?: string;
  fieldErrors?: Record<string, string>;
};

export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;

  constructor(
    message: string,
    status: number,
    fieldErrors: Record<string, string> = {},
  ) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");
const apiUrl = (path: string) => `${apiBaseUrl}${path}`;
const credentialsMode: RequestCredentials = apiBaseUrl
  ? "include"
  : "same-origin";

function csrfCookie(): string | null {
  const cookie = document.cookie
    .split("; ")
    .find((part) => part.startsWith("XSRF-TOKEN="));
  return cookie ? decodeURIComponent(cookie.slice("XSRF-TOKEN=".length)) : null;
}

async function bootstrapCsrf(): Promise<void> {
  const response = await fetch(apiUrl("/api/v1/auth/csrf"), {
    credentials: credentialsMode,
  });
  if (!response.ok)
    throw new ApiError("Could not prepare a secure request.", response.status);
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type"))
    headers.set("Content-Type", "application/json");

  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    if (!csrfCookie()) await bootstrapCsrf();
    const token = csrfCookie();
    if (!token) throw new ApiError("Could not prepare a secure request.", 0);
    headers.set("X-XSRF-TOKEN", token);
  }

  const response = await fetch(apiUrl(path), {
    ...init,
    method,
    headers,
    credentials: credentialsMode,
  });
  if (response.status === 204) return undefined as T;

  const body = (await response.json().catch(() => ({}))) as ApiErrorBody & T;
  if (!response.ok) {
    throw new ApiError(
      body.message ?? "The request could not be completed.",
      response.status,
      body.fieldErrors,
    );
  }
  return body;
}

export async function register(input: RegistrationInput): Promise<User> {
  return request<User>("/api/v1/auth/register", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export async function login(email: string, password: string): Promise<User> {
  const user = await request<User>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
  await bootstrapCsrf();
  return user;
}

export async function currentUser(): Promise<User> {
  return request<User>("/api/v1/users/me");
}

export async function updateDisplayName(displayName: string): Promise<User> {
  return request<User>("/api/v1/users/me", {
    method: "PATCH",
    body: JSON.stringify({ displayName }),
  });
}

export async function logout(): Promise<void> {
  await request<void>("/api/v1/auth/logout", { method: "POST" });
}

export async function listWorkspaces(): Promise<Workspace[]> {
  return request<Workspace[]>("/api/v1/workspaces");
}

export async function createWorkspace(
  name: string,
  description: string,
): Promise<Workspace> {
  return request<Workspace>("/api/v1/workspaces", {
    method: "POST",
    body: JSON.stringify({ name, description: description || null }),
  });
}

export async function updateWorkspace(
  id: string,
  name: string,
  description: string,
): Promise<Workspace> {
  return request<Workspace>(`/api/v1/workspaces/${encodeURIComponent(id)}`, {
    method: "PATCH",
    body: JSON.stringify({ name, description: description || null }),
  });
}

export async function deleteWorkspace(id: string): Promise<void> {
  return request<void>(`/api/v1/workspaces/${encodeURIComponent(id)}`, {
    method: "DELETE",
  });
}

export async function listWorkspaceMembers(
  workspaceId: string,
): Promise<WorkspaceMember[]> {
  return request<WorkspaceMember[]>(
    `/api/v1/workspaces/${encodeURIComponent(workspaceId)}/members`,
  );
}

export async function addWorkspaceMember(
  workspaceId: string,
  email: string,
  role: Exclude<MembershipRole, "OWNER">,
): Promise<WorkspaceMember> {
  return request<WorkspaceMember>(
    `/api/v1/workspaces/${encodeURIComponent(workspaceId)}/members`,
    {
      method: "POST",
      body: JSON.stringify({ email, role }),
    },
  );
}

export async function updateWorkspaceMemberRole(
  workspaceId: string,
  userId: string,
  role: Exclude<MembershipRole, "OWNER">,
): Promise<WorkspaceMember> {
  return request<WorkspaceMember>(
    `/api/v1/workspaces/${encodeURIComponent(workspaceId)}/members/${encodeURIComponent(userId)}`,
    { method: "PATCH", body: JSON.stringify({ role }) },
  );
}

export async function removeWorkspaceMember(
  workspaceId: string,
  userId: string,
): Promise<void> {
  return request<void>(
    `/api/v1/workspaces/${encodeURIComponent(workspaceId)}/members/${encodeURIComponent(userId)}`,
    { method: "DELETE" },
  );
}

export async function listProjects(workspaceId: string): Promise<Project[]> {
  return request<Project[]>(
    `/api/v1/workspaces/${encodeURIComponent(workspaceId)}/projects`,
  );
}

export async function createProject(
  workspaceId: string,
  name: string,
  key: string,
  description: string,
): Promise<Project> {
  return request<Project>(
    `/api/v1/workspaces/${encodeURIComponent(workspaceId)}/projects`,
    {
      method: "POST",
      body: JSON.stringify({ name, key, description: description || null }),
    },
  );
}

export async function updateProject(
  projectId: string,
  name: string,
  description: string,
): Promise<Project> {
  return request<Project>(`/api/v1/projects/${encodeURIComponent(projectId)}`, {
    method: "PATCH",
    body: JSON.stringify({ name, description: description || null }),
  });
}

export async function updateProjectStatus(
  projectId: string,
  status: ProjectStatus,
): Promise<Project> {
  return request<Project>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/status`,
    {
      method: "PATCH",
      body: JSON.stringify({ status }),
    },
  );
}

export async function listIssues(
  projectId: string,
  filters: IssueFilters = {},
): Promise<IssuePage> {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.set(key, String(value));
  });
  return request<IssuePage>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/issues?${query}`,
  );
}
export async function listBacklog(
  projectId: string,
  filters: Omit<IssueFilters, "status" | "sort" | "direction"> = {},
): Promise<IssuePage> {
  const query = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.set(key, String(value));
  });
  return request<IssuePage>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/backlog?${query}`,
  );
}
export async function listProjectSprints(projectId: string): Promise<Sprint[]> {
  return request<Sprint[]>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/sprints`,
  );
}
export async function createSprint(
  projectId: string,
  input: SprintInput,
): Promise<Sprint> {
  return request<Sprint>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/sprints`,
    { method: "POST", body: JSON.stringify(input) },
  );
}
export async function updateSprint(
  projectId: string,
  sprintId: string,
  input: SprintInput,
): Promise<Sprint> {
  return request<Sprint>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/sprints/${encodeURIComponent(sprintId)}`,
    { method: "PATCH", body: JSON.stringify(input) },
  );
}
export async function startSprint(
  projectId: string,
  sprintId: string,
): Promise<Sprint> {
  return request<Sprint>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/sprints/${encodeURIComponent(sprintId)}/start`,
    { method: "POST" },
  );
}
export async function completeSprint(
  projectId: string,
  sprintId: string,
  unfinishedIssueAction: "BACKLOG" | "FUTURE_SPRINT",
  targetSprintId: string | null,
): Promise<Sprint> {
  return request<Sprint>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/sprints/${encodeURIComponent(sprintId)}/complete`,
    {
      method: "POST",
      body: JSON.stringify({ unfinishedIssueAction, targetSprintId }),
    },
  );
}
export async function addIssuesToSprint(
  projectId: string,
  sprintId: string,
  issueIds: string[],
): Promise<void> {
  return request<void>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/sprints/${encodeURIComponent(sprintId)}/issues`,
    { method: "POST", body: JSON.stringify({ issueIds }) },
  );
}
export async function removeIssuesFromSprint(
  projectId: string,
  sprintId: string,
  issueIds: string[],
): Promise<void> {
  return request<void>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/sprints/${encodeURIComponent(sprintId)}/remove-issues`,
    { method: "POST", body: JSON.stringify({ issueIds }) },
  );
}
export async function listSprintIssues(
  projectId: string,
  sprintId: string,
  status: IssueStatus,
  page = 0,
  size = 50,
  filters: IssueFilters = {},
): Promise<IssuePage> {
  const query = new URLSearchParams({
    status,
    page: String(page),
    size: String(size),
  });
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.set(key, String(value));
  });
  return request<IssuePage>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/sprints/${encodeURIComponent(sprintId)}/issues?${query}`,
  );
}
export async function listSprintIssuesAll(
  projectId: string,
  sprintId: string,
): Promise<Issue[]> {
  const result = await request<IssuePage>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/sprints/${encodeURIComponent(sprintId)}/issues?size=100`,
  );
  return result.items;
}
export async function assignIssueToSprint(
  issueId: string,
  sprintId: string | null,
  beforeIssueId: string | null = null,
): Promise<Issue> {
  return request<Issue>(
    `/api/v1/issues/${encodeURIComponent(issueId)}/sprint`,
    {
      method: "PATCH",
      body: JSON.stringify({ sprintId, beforeIssueId }),
    },
  );
}
export async function getIssue(issueId: string): Promise<Issue> {
  return request<Issue>(`/api/v1/issues/${encodeURIComponent(issueId)}`);
}
export async function getIssueByKey(
  workspaceId: string,
  issueKey: string,
): Promise<Issue> {
  return request<Issue>(
    `/api/v1/workspaces/${encodeURIComponent(workspaceId)}/issues/${encodeURIComponent(issueKey)}`,
  );
}
export async function getIssueReference(
  workspaceId: string,
  reference: string,
): Promise<Issue> {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(
    reference,
  )
    ? getIssue(reference)
    : getIssueByKey(workspaceId, reference);
}

export type IssueInput = {
  title: string;
  description: string | null;
  type: IssueType;
  status: IssueStatus;
  priority: IssuePriority;
  assigneeId: string | null;
  dueDate: string | null;
};
export async function createIssue(
  projectId: string,
  input: IssueInput,
): Promise<Issue> {
  return request<Issue>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/issues`,
    { method: "POST", body: JSON.stringify(input) },
  );
}
export async function updateIssue(
  issueId: string,
  input: IssueInput,
): Promise<Issue> {
  return request<Issue>(`/api/v1/issues/${encodeURIComponent(issueId)}`, {
    method: "PATCH",
    body: JSON.stringify(input),
  });
}
export async function moveIssue(
  issueId: string,
  status: IssueStatus,
  beforeIssueId: string | null,
): Promise<Issue> {
  return request<Issue>(
    `/api/v1/issues/${encodeURIComponent(issueId)}/position`,
    {
      method: "PATCH",
      body: JSON.stringify({ status, beforeIssueId }),
    },
  );
}
export async function archiveIssue(issueId: string): Promise<void> {
  return request<void>(`/api/v1/issues/${encodeURIComponent(issueId)}`, {
    method: "DELETE",
  });
}
export async function restoreIssue(issueId: string): Promise<Issue> {
  return request<Issue>(
    `/api/v1/issues/${encodeURIComponent(issueId)}/restore`,
    { method: "POST" },
  );
}

export async function listIssueComments(
  issueId: string,
): Promise<PageResult<IssueComment>> {
  return request<PageResult<IssueComment>>(
    `/api/v1/issues/${encodeURIComponent(issueId)}/comments?size=50`,
  );
}
export async function addIssueComment(
  issueId: string,
  body: string,
): Promise<IssueComment> {
  return request<IssueComment>(
    `/api/v1/issues/${encodeURIComponent(issueId)}/comments`,
    { method: "POST", body: JSON.stringify({ body }) },
  );
}
export async function listIssueActivities(
  issueId: string,
): Promise<PageResult<IssueActivity>> {
  return request<PageResult<IssueActivity>>(
    `/api/v1/issues/${encodeURIComponent(issueId)}/activities?size=50`,
  );
}
export async function listProjectEpics(projectId: string): Promise<Epic[]> {
  return request<Epic[]>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/epics`,
  );
}
export async function createEpic(
  projectId: string,
  input: EpicInput,
): Promise<Epic> {
  return request<Epic>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/epics`,
    { method: "POST", body: JSON.stringify(input) },
  );
}
export async function updateEpic(
  projectId: string,
  epicId: string,
  input: EpicInput,
): Promise<Epic> {
  return request<Epic>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/epics/${encodeURIComponent(epicId)}`,
    { method: "PUT", body: JSON.stringify(input) },
  );
}
export async function listEpicIssues(
  projectId: string,
  epicId: string,
  status?: IssueStatus,
  search = "",
): Promise<IssuePage> {
  const query = new URLSearchParams({ size: "100" });
  if (status) query.set("status", status);
  if (search.trim()) query.set("search", search.trim());
  return request<IssuePage>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/epics/${encodeURIComponent(epicId)}/issues?${query}`,
  );
}
export async function listDueIssues(projectId: string): Promise<IssuePage> {
  return request<IssuePage>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/timeline/due-issues?size=100`,
  );
}
export async function listProjectReleases(
  projectId: string,
): Promise<ProjectRelease[]> {
  return request<ProjectRelease[]>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/releases`,
  );
}
export async function createRelease(
  projectId: string,
  input: ReleaseInput,
): Promise<ProjectRelease> {
  return request<ProjectRelease>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/releases`,
    { method: "POST", body: JSON.stringify(input) },
  );
}
export async function updateRelease(
  projectId: string,
  releaseId: string,
  input: ReleaseInput,
): Promise<ProjectRelease> {
  return request<ProjectRelease>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/releases/${encodeURIComponent(releaseId)}`,
    { method: "PUT", body: JSON.stringify(input) },
  );
}
export async function assignIssueEpic(
  issueId: string,
  id: string | null,
): Promise<Issue> {
  return request<Issue>(`/api/v1/issues/${encodeURIComponent(issueId)}/epic`, {
    method: "PUT",
    body: JSON.stringify({ id }),
  });
}
export async function assignIssueParent(
  issueId: string,
  id: string | null,
): Promise<Issue> {
  return request<Issue>(
    `/api/v1/issues/${encodeURIComponent(issueId)}/parent`,
    { method: "PUT", body: JSON.stringify({ id }) },
  );
}
export async function assignIssueRelease(
  issueId: string,
  id: string | null,
): Promise<Issue> {
  return request<Issue>(
    `/api/v1/issues/${encodeURIComponent(issueId)}/release`,
    { method: "PUT", body: JSON.stringify({ id }) },
  );
}
export async function getIssueRelationships(
  issueId: string,
): Promise<IssueRelationships> {
  return request<IssueRelationships>(
    `/api/v1/issues/${encodeURIComponent(issueId)}/relationships`,
  );
}
export async function addIssueLink(
  issueId: string,
  targetIssueId: string,
  type: IssueLinkType,
): Promise<IssueLink> {
  return request<IssueLink>(
    `/api/v1/issues/${encodeURIComponent(issueId)}/links`,
    { method: "POST", body: JSON.stringify({ targetIssueId, type }) },
  );
}
export async function removeIssueLink(
  issueId: string,
  linkId: string,
): Promise<void> {
  return request<void>(
    `/api/v1/issues/${encodeURIComponent(issueId)}/links/${encodeURIComponent(linkId)}`,
    { method: "DELETE" },
  );
}
export async function listNotifications(
  unreadOnly = false,
): Promise<PageResult<Notification>> {
  return request<PageResult<Notification>>(
    `/api/v1/users/me/notifications?size=20&unreadOnly=${unreadOnly}`,
  );
}
export async function markNotificationRead(
  notificationId: string,
): Promise<Notification> {
  return request<Notification>(
    `/api/v1/notifications/${encodeURIComponent(notificationId)}/read`,
    { method: "PATCH" },
  );
}

export async function initializeSession(): Promise<User | null> {
  await bootstrapCsrf();
  try {
    return await currentUser();
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      try {
        await logout();
      } catch {
        // The next sign-in request can retry server-side cookie cleanup if the network was unavailable.
      }
      return null;
    }
    throw error;
  }
}
