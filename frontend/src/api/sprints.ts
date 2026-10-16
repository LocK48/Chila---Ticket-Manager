import { request } from "./client";
import type {
  Issue,
  IssueFilters,
  IssuePage,
  IssueStatus,
  Sprint,
  SprintInput,
} from "./types";

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
