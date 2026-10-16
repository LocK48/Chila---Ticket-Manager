import { request } from "./client";
import type { Project, ProjectStatus } from "./types";

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
