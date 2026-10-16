import { request } from "./client";
import type { IssueActivity, IssueComment, PageResult } from "./types";

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
