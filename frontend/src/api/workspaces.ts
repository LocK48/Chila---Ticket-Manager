import { request } from "./client";
import type { MembershipRole, Workspace, WorkspaceMember } from "./types";

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
