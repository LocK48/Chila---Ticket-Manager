import { request } from "./client";
import type {
  Epic,
  EpicInput,
  Issue,
  IssueLink,
  IssueLinkType,
  IssuePage,
  IssueRelationships,
  IssueStatus,
  ProjectRelease,
  ReleaseInput,
} from "./types";

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
