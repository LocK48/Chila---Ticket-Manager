import { request } from "./client";
import type { ProjectDashboard, SprintReport } from "./types";

export async function getProjectDashboard(
  projectId: string,
): Promise<ProjectDashboard> {
  return request<ProjectDashboard>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/reports`,
  );
}

export async function getSprintReport(
  projectId: string,
  sprintId: string,
): Promise<SprintReport> {
  return request<SprintReport>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/reports/sprints/${encodeURIComponent(sprintId)}`,
  );
}
