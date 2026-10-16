import { request } from "./client";
import type { AutomationExecution, AutomationRule, AutomationRuleInput } from "./types";

export async function listAutomationRules(
  projectId: string,
): Promise<AutomationRule[]> {
  return request<AutomationRule[]>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/automation/rules`,
  );
}

export async function createAutomationRule(
  projectId: string,
  input: AutomationRuleInput,
): Promise<AutomationRule> {
  return request<AutomationRule>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/automation/rules`,
    { method: "POST", body: JSON.stringify(input) },
  );
}

export async function updateAutomationRule(
  projectId: string,
  ruleId: string,
  input: AutomationRuleInput,
): Promise<AutomationRule> {
  return request<AutomationRule>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/automation/rules/${encodeURIComponent(ruleId)}`,
    { method: "PUT", body: JSON.stringify(input) },
  );
}

export async function deleteAutomationRule(
  projectId: string,
  ruleId: string,
): Promise<void> {
  return request<void>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/automation/rules/${encodeURIComponent(ruleId)}`,
    { method: "DELETE" },
  );
}

export async function listAutomationExecutions(
  projectId: string,
): Promise<AutomationExecution[]> {
  return request<AutomationExecution[]>(
    `/api/v1/projects/${encodeURIComponent(projectId)}/automation/executions`,
  );
}
