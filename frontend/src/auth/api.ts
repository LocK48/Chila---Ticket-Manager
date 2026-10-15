export type User = {
  id: string
  email: string
  displayName: string
  createdAt: string
}

export type MembershipRole = 'OWNER' | 'ADMIN' | 'MEMBER'

export type Workspace = {
  id: string
  name: string
  description: string | null
  role: MembershipRole
  createdAt: string
  updatedAt: string
}

export type WorkspaceMember = {
  userId: string
  email: string
  displayName: string
  role: MembershipRole
  joinedAt: string
}

export type ProjectStatus = 'ACTIVE' | 'ARCHIVED'

export type Project = {
  id: string
  workspaceId: string
  createdBy: string
  name: string
  key: string
  description: string | null
  status: ProjectStatus
  createdAt: string
  updatedAt: string
}

export type IssueType = 'TASK' | 'BUG' | 'STORY'
export type IssueStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE'
export type IssuePriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
export type Issue = {
  id: string; projectId: string; projectKey: string; title: string; description: string | null
  type: IssueType; status: IssueStatus; priority: IssuePriority; reporterId: string
  reporterName: string; assigneeId: string | null; assigneeName: string | null
  dueDate: string | null; archivedAt: string | null; createdAt: string; updatedAt: string
}
export type IssuePage = { items: Issue[]; page: number; size: number; totalItems: number; totalPages: number }
export type IssueFilters = {
  status?: IssueStatus; priority?: IssuePriority; type?: IssueType; assigneeId?: string
  unassigned?: boolean; search?: string; includeArchived?: boolean; page?: number
  size?: number; sort?: string; direction?: 'asc' | 'desc'
}

export type RegistrationInput = {
  email: string
  password: string
  displayName: string
}

export type ApiErrorBody = {
  code?: string
  message?: string
  fieldErrors?: Record<string, string>
}

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(
    message: string,
    status: number,
    fieldErrors: Record<string, string> = {},
  ) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

function csrfCookie(): string | null {
  const cookie = document.cookie.split('; ').find((part) => part.startsWith('XSRF-TOKEN='))
  return cookie ? decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) : null
}

async function bootstrapCsrf(): Promise<void> {
  const response = await fetch('/api/v1/auth/csrf', { credentials: 'same-origin' })
  if (!response.ok) throw new ApiError('Could not prepare a secure request.', response.status)
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    if (!csrfCookie()) await bootstrapCsrf()
    const token = csrfCookie()
    if (!token) throw new ApiError('Could not prepare a secure request.', 0)
    headers.set('X-XSRF-TOKEN', token)
  }

  const response = await fetch(path, { ...init, method, headers, credentials: 'same-origin' })
  if (response.status === 204) return undefined as T

  const body = (await response.json().catch(() => ({}))) as ApiErrorBody & T
  if (!response.ok) {
    throw new ApiError(body.message ?? 'The request could not be completed.', response.status, body.fieldErrors)
  }
  return body
}

export async function register(input: RegistrationInput): Promise<User> {
  return request<User>('/api/v1/auth/register', { method: 'POST', body: JSON.stringify(input) })
}

export async function login(email: string, password: string): Promise<User> {
  const user = await request<User>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
  await bootstrapCsrf()
  return user
}

export async function currentUser(): Promise<User> {
  return request<User>('/api/v1/users/me')
}

export async function updateDisplayName(displayName: string): Promise<User> {
  return request<User>('/api/v1/users/me', {
    method: 'PATCH',
    body: JSON.stringify({ displayName }),
  })
}

export async function logout(): Promise<void> {
  await request<void>('/api/v1/auth/logout', { method: 'POST' })
}

export async function listWorkspaces(): Promise<Workspace[]> {
  return request<Workspace[]>('/api/v1/workspaces')
}

export async function createWorkspace(name: string, description: string): Promise<Workspace> {
  return request<Workspace>('/api/v1/workspaces', {
    method: 'POST',
    body: JSON.stringify({ name, description: description || null }),
  })
}

export async function updateWorkspace(id: string, name: string, description: string): Promise<Workspace> {
  return request<Workspace>(`/api/v1/workspaces/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    body: JSON.stringify({ name, description: description || null }),
  })
}

export async function deleteWorkspace(id: string): Promise<void> {
  return request<void>(`/api/v1/workspaces/${encodeURIComponent(id)}`, { method: 'DELETE' })
}

export async function listWorkspaceMembers(workspaceId: string): Promise<WorkspaceMember[]> {
  return request<WorkspaceMember[]>(`/api/v1/workspaces/${encodeURIComponent(workspaceId)}/members`)
}

export async function addWorkspaceMember(
  workspaceId: string,
  email: string,
  role: Exclude<MembershipRole, 'OWNER'>,
): Promise<WorkspaceMember> {
  return request<WorkspaceMember>(`/api/v1/workspaces/${encodeURIComponent(workspaceId)}/members`, {
    method: 'POST',
    body: JSON.stringify({ email, role }),
  })
}

export async function updateWorkspaceMemberRole(
  workspaceId: string,
  userId: string,
  role: Exclude<MembershipRole, 'OWNER'>,
): Promise<WorkspaceMember> {
  return request<WorkspaceMember>(
    `/api/v1/workspaces/${encodeURIComponent(workspaceId)}/members/${encodeURIComponent(userId)}`,
    { method: 'PATCH', body: JSON.stringify({ role }) },
  )
}

export async function removeWorkspaceMember(workspaceId: string, userId: string): Promise<void> {
  return request<void>(
    `/api/v1/workspaces/${encodeURIComponent(workspaceId)}/members/${encodeURIComponent(userId)}`,
    { method: 'DELETE' },
  )
}

export async function listProjects(workspaceId: string): Promise<Project[]> {
  return request<Project[]>(`/api/v1/workspaces/${encodeURIComponent(workspaceId)}/projects`)
}

export async function createProject(
  workspaceId: string,
  name: string,
  key: string,
  description: string,
): Promise<Project> {
  return request<Project>(`/api/v1/workspaces/${encodeURIComponent(workspaceId)}/projects`, {
    method: 'POST',
    body: JSON.stringify({ name, key, description: description || null }),
  })
}

export async function updateProject(
  projectId: string,
  name: string,
  description: string,
): Promise<Project> {
  return request<Project>(`/api/v1/projects/${encodeURIComponent(projectId)}`, {
    method: 'PATCH',
    body: JSON.stringify({ name, description: description || null }),
  })
}

export async function updateProjectStatus(projectId: string, status: ProjectStatus): Promise<Project> {
  return request<Project>(`/api/v1/projects/${encodeURIComponent(projectId)}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })
}

export async function listIssues(projectId: string, filters: IssueFilters = {}): Promise<IssuePage> {
  const query = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => { if (value !== undefined && value !== '') query.set(key, String(value)) })
  return request<IssuePage>(`/api/v1/projects/${encodeURIComponent(projectId)}/issues?${query}`)
}

export type IssueInput = {
  title: string; description: string | null; type: IssueType; status: IssueStatus
  priority: IssuePriority; assigneeId: string | null; dueDate: string | null
}
export async function createIssue(projectId: string, input: IssueInput): Promise<Issue> {
  return request<Issue>(`/api/v1/projects/${encodeURIComponent(projectId)}/issues`, { method: 'POST', body: JSON.stringify(input) })
}
export async function updateIssue(issueId: string, input: IssueInput): Promise<Issue> {
  return request<Issue>(`/api/v1/issues/${encodeURIComponent(issueId)}`, { method: 'PATCH', body: JSON.stringify(input) })
}
export async function archiveIssue(issueId: string): Promise<void> {
  return request<void>(`/api/v1/issues/${encodeURIComponent(issueId)}`, { method: 'DELETE' })
}
export async function restoreIssue(issueId: string): Promise<Issue> {
  return request<Issue>(`/api/v1/issues/${encodeURIComponent(issueId)}/restore`, { method: 'POST' })
}

export async function initializeSession(): Promise<User | null> {
  await bootstrapCsrf()
  try {
    return await currentUser()
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      try {
        await logout()
      } catch {
        // The next sign-in request can retry server-side cookie cleanup if the network was unavailable.
      }
      return null
    }
    throw error
  }
}
