import { request } from "./client";
import type {
  SavedFilter,
  SavedFilterInput,
  Workflow,
  WorkflowTransition,
} from "./types";

export async function listSavedFilters(
  projectId: string,
): Promise<SavedFilter[]> {
  return request<SavedFilter[]>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/filters`,
  );
}

export async function createSavedFilter(
  projectId: string,
  input: SavedFilterInput,
): Promise<SavedFilter> {
  return request<SavedFilter>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/filters`,
    { method: "POST", body: JSON.stringify(input) },
  );
}

export async function updateSavedFilter(
  projectId: string,
  filterId: string,
  input: SavedFilterInput,
): Promise<SavedFilter> {
  return request<SavedFilter>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/filters/${encodeURIComponent(filterId)}`,
    { method: "PUT", body: JSON.stringify(input) },
  );
}

export async function deleteSavedFilter(
  projectId: string,
  filterId: string,
): Promise<void> {
  return request<void>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/filters/${encodeURIComponent(filterId)}`,
    { method: "DELETE" },
  );
}

export async function getWorkflow(projectId: string): Promise<Workflow> {
  return request<Workflow>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/workflow`,
  );
}

export async function updateWorkflow(
  projectId: string,
  transitions: WorkflowTransition[],
): Promise<Workflow> {
  return request<Workflow>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/workflow/transitions`,
    { method: "PUT", body: JSON.stringify(transitions) },
  );
}
