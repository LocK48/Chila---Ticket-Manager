import { request } from "./client";
import type {
  Issue,
  IssueFilters,
  IssueInput,
  IssuePage,
  IssueStatus,
} from "./types";

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
