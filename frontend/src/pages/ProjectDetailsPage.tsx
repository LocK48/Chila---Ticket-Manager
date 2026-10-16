import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  Link,
  NavLink,
  Outlet,
  useOutletContext,
  useParams,
} from "react-router-dom";
import { ApiError } from "../api/client";
import { getProjectDashboard } from "../api/reports";
import { getWorkflow, updateWorkflow } from "../api/filters";
import { listAutomationRules, listAutomationExecutions, createAutomationRule, updateAutomationRule, deleteAutomationRule } from "../api/automation";
import { listProjects, updateProject } from "../api/projects";
import { listWorkspaceMembers } from "../api/workspaces";
import { type Project, type WorkspaceMember, type IssueStatus, type WorkflowTransition, type ProjectDashboard, type AutomationRule, type AutomationExecution } from "../api/types";
import { IssuesPanel } from "../features/issues/IssuesPanel";
import { EpicsPage, ReleasesPage } from "../features/hierarchy/HierarchyPages";
import { KanbanBoard } from "../features/issues/KanbanBoard";
import { BacklogView, ActiveSprintView } from "../features/sprints/SprintPages";
import { PageHeader } from "../components/ui/PageHeader";
import { ErrorState, LoadingState } from "../components/ui/StateMessage";
import { useAppLayout } from "../layouts/app-layout-context";

export function ProjectDetailsPage() {
  const { workspaceId = "", projectId = "" } = useParams();
  const { activeWorkspace } = useAppLayout();
  const [project, setProject] = useState<Project | null>(null);
  const [members, setMembers] = useState<WorkspaceMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [projects, workspaceMembers] = await Promise.all([
        listProjects(workspaceId),
        listWorkspaceMembers(workspaceId),
      ]);
      setProject(projects.find((item) => item.id === projectId) ?? null);
      setMembers(workspaceMembers);
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not load this project.",
      );
    } finally {
      setLoading(false);
    }
  }, [projectId, workspaceId]);
  useEffect(() => {
    void Promise.resolve().then(load);
  }, [load]);
  if (loading) return <LoadingState>Loading project…</LoadingState>;
  if (error) return <ErrorState message={error} onRetry={() => void load()} />;
  if (!project)
    return (
      <section className="access-state">
        <h1>Project unavailable</h1>
        <p>This project was not found in the selected workspace.</p>
        <Link
          className="secondary-button"
          to={`/workspaces/${workspaceId}/projects`}
        >
          Back to projects
        </Link>
      </section>
    );
  const canManage = activeWorkspace?.role !== "MEMBER";
  return (
    <>
      <p className="breadcrumb">
        <Link to={`/workspaces/${workspaceId}/projects`}>Projects</Link>
        <span>/</span>
        {project.key}
      </p>
      <PageHeader
        eyebrow={project.key}
        title={project.name}
        description={project.description || "No project description."}
        action={
          canManage ? (
            <Link
              className="secondary-button"
              to={`/workspaces/${workspaceId}/projects/${project.id}/settings`}
            >
              Project settings
            </Link>
          ) : undefined
        }
      />
      <nav className="project-tabs" aria-label="Project pages">
        <NavLink end to={`/workspaces/${workspaceId}/projects/${project.id}`}>
          Issues
        </NavLink>
        <NavLink to={`/workspaces/${workspaceId}/projects/${project.id}/board`}>
          Board
        </NavLink>
        <NavLink
          to={`/workspaces/${workspaceId}/projects/${project.id}/backlog`}
        >
          Backlog
        </NavLink>
        <NavLink
          to={`/workspaces/${workspaceId}/projects/${project.id}/active-sprint`}
        >
          Active sprint
        </NavLink>
        <NavLink to={`/workspaces/${workspaceId}/projects/${project.id}/epics`}>
          Epics
        </NavLink>
        <NavLink
          to={`/workspaces/${workspaceId}/projects/${project.id}/releases`}
        >
          Releases
        </NavLink>
        <NavLink
          to={`/workspaces/${workspaceId}/projects/${project.id}/overview`}
        >
          Overview
        </NavLink>
        <NavLink
          to={`/workspaces/${workspaceId}/projects/${project.id}/reports`}
        >
          Reports
        </NavLink>
        {canManage && (
          <NavLink
            to={`/workspaces/${workspaceId}/projects/${project.id}/settings`}
          >
            Settings
          </NavLink>
        )}
      </nav>
      <Outlet context={{ project, members, canManage, setProject }} />
    </>
  );
}

export function ProjectIssuesTab() {
  const { project, members, canManage } = useProjectContext();
  return (
    <IssuesPanel
      projectId={project.id}
      members={members}
      canManage={canManage}
      active
      projectActive={project.status === "ACTIVE"}
    />
  );
}

export function ProjectOverviewTab() {
  const { project } = useProjectContext();
  const [dashboard, setDashboard] = useState<ProjectDashboard | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setDashboard(await getProjectDashboard(project.id));
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not load project reports.",
      );
    } finally {
      setLoading(false);
    }
  }, [project.id]);
  useEffect(() => {
    void Promise.resolve().then(load);
  }, [load]);
  const statusLabels: Record<IssueStatus, string> = {
    TODO: "To do",
    IN_PROGRESS: "In progress",
    IN_REVIEW: "In review",
    DONE: "Done",
  };
  return (
    <div className="report-page">
      <div className="report-heading">
        <div>
          <p className="eyebrow">PROJECT REPORTS</p>
          <h2>Project dashboard</h2>
          <p className="subtle">
            A live summary of issue flow and completed sprint history.
          </p>
        </div>
        <button
          className="secondary-button compact"
          onClick={() => void load()}
          disabled={loading}
        >
          Refresh
        </button>
      </div>
      {loading ? (
        <p className="subtle">Loading report…</p>
      ) : error ? (
        <div className="report-error" role="alert">
          {error}
          <button className="text-action" onClick={() => void load()}>
            Try again
          </button>
        </div>
      ) : (
        dashboard && (
          <>
            <div className="report-stat-grid">
              <ReportStat
                label="Open issues"
                value={dashboard.totalIssues - dashboard.issuesByStatus.DONE}
                note={`${dashboard.totalIssues} issues total`}
              />
              <ReportStat
                label="Overdue"
                value={dashboard.overdueIssues}
                note="Unarchived issues past due date"
              />
              <ReportStat
                label="Completed recently"
                value={dashboard.completedLast30Days}
                note="In the last 30 days"
              />
              <ReportStat
                label="Average lead time"
                value={
                  dashboard.averageLeadTimeDays == null
                    ? "—"
                    : `${dashboard.averageLeadTimeDays.toFixed(1)} days`
                }
                note="Issue creation to first completion"
              />
              <ReportStat
                label="Average cycle time"
                value={
                  dashboard.averageCycleTimeDays == null
                    ? "—"
                    : `${dashboard.averageCycleTimeDays.toFixed(1)} days`
                }
                note="First in progress to first completion"
              />
            </div>
            <section className="report-card">
              <div className="report-section-title">
                <div>
                  <h3>Issues by status</h3>
                  <p className="subtle">Current unarchived issue counts</p>
                </div>
              </div>
              <div className="status-report-grid">
                {(Object.keys(statusLabels) as IssueStatus[]).map((status) => {
                  const count = dashboard.issuesByStatus[status] ?? 0;
                  const max = Math.max(
                    1,
                    ...Object.values(dashboard.issuesByStatus),
                  );
                  return (
                    <div className="status-report-row" key={status}>
                      <span>{statusLabels[status]}</span>
                      <div className="report-track">
                        <i style={{ width: `${(count / max) * 100}%` }} />
                      </div>
                      <strong>{count}</strong>
                    </div>
                  );
                })}
              </div>
            </section>
            <section className="report-card">
              <div className="report-section-title">
                <div>
                  <h3>Sprint velocity</h3>
                  <p className="subtle">
                    Completed issues from sprint completion snapshots; issue
                    counts are not story points.
                  </p>
                </div>
              </div>
              {dashboard.sprintVelocity.length ? (
                <div className="report-table-wrap">
                  <table className="report-table">
                    <thead>
                      <tr>
                        <th>Sprint</th>
                        <th>Committed</th>
                        <th>Completed</th>
                        <th>Unfinished</th>
                        <th>Completion</th>
                      </tr>
                    </thead>
                    <tbody>
                      {dashboard.sprintVelocity.map((sprint) => (
                        <tr key={sprint.sprintId}>
                          <th scope="row">{sprint.sprintName}</th>
                          {sprint.historyAvailable ? (
                            <>
                              <td>{sprint.committedIssues}</td>
                              <td>{sprint.completedIssues}</td>
                              <td>{sprint.unfinishedIssues}</td>
                              <td>
                                {sprint.completionPercentage?.toFixed(0)}%
                              </td>
                            </>
                          ) : (
                            <td colSpan={4} className="subtle">
                              Historical scope unavailable
                            </td>
                          )}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="report-empty">
                  Complete a sprint to see velocity here.
                </p>
              )}
            </section>
          </>
        )
      )}
    </div>
  );
}

function ReportStat({
  label,
  value,
  note,
}: {
  label: string;
  value: string | number;
  note: string;
}) {
  return (
    <article className="report-stat">
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{note}</small>
    </article>
  );
}

export function ProjectBoardTab() {
  const { project, members } = useProjectContext();
  return (
    <KanbanBoard
      projectId={project.id}
      members={members}
      projectActive={project.status === "ACTIVE"}
    />
  );
}

export function ProjectBacklogRoute() {
  const { project, members, canManage } = useProjectContext();
  return (
    <BacklogView
      project={project}
      members={members}
      canManage={canManage}
      projectActive={project.status === "ACTIVE"}
    />
  );
}
export function ProjectActiveSprintRoute() {
  const { project, members, canManage } = useProjectContext();
  return (
    <ActiveSprintView
      project={project}
      members={members}
      canManage={canManage}
      projectActive={project.status === "ACTIVE"}
    />
  );
}

export function ProjectEpicsRoute() {
  const { project, canManage } = useProjectContext();
  return <EpicsPage project={project} canManage={canManage} />;
}

export function ProjectReleasesRoute() {
  const { project, canManage } = useProjectContext();
  return <ReleasesPage project={project} canManage={canManage} />;
}

export function ProjectSettingsTab() {
  const { workspaceId = "" } = useParams();
  const { project, canManage, setProject } = useProjectContext();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    setBusy(true);
    setError("");
    try {
      setProject(
        await updateProject(
          project.id,
          String(data.get("name")).trim(),
          String(data.get("description") ?? "").trim(),
        ),
      );
      setNotice("Project settings saved.");
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not update project settings.",
      );
    } finally {
      setBusy(false);
    }
  }
  if (!canManage)
    return (
      <section className="access-state">
        <h2>Project settings are restricted</h2>
        <p>Ask a workspace admin or owner to update this project.</p>
      </section>
    );
  return (
    <>
      <form
        className="workspace-form settings-form"
        onSubmit={(event) => void save(event)}
      >
        <h2>Project settings</h2>
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        {notice && (
          <p className="notice" role="status">
            {notice}
          </p>
        )}
        <label>
          Project name
          <input
            name="name"
            required
            maxLength={100}
            defaultValue={project.name}
            key={project.updatedAt}
          />
        </label>
        <label>
          Project key
          <input value={project.key} readOnly />
        </label>
        <label>
          Description
          <textarea
            name="description"
            rows={4}
            maxLength={500}
            defaultValue={project.description ?? ""}
            key={`${project.id}-${project.updatedAt}`}
          />
        </label>
        <p className="member-note">
          Project key is permanent. To archive or restore this project, use the
          projects list.
        </p>
        <button className="primary-button compact" disabled={busy}>
          {busy ? "Saving…" : "Save changes"}
        </button>
      </form>
      <Link
        className="secondary-button"
        to={`/workspaces/${workspaceId}/projects`}
      >
        Back to projects
      </Link>
      <WorkflowSettings projectId={project.id} />
      <AutomationSettings projectId={project.id} />
    </>
  );
}

function AutomationSettings({ projectId }: { projectId: string }) {
  const [rules, setRules] = useState<AutomationRule[]>([]);
  const [executions, setExecutions] = useState<AutomationExecution[]>([]);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const statuses: IssueStatus[] = ["TODO", "IN_PROGRESS", "IN_REVIEW", "DONE"];
  const load = useCallback(async () => {
    setError("");
    try {
      const [nextRules, nextExecutions] = await Promise.all([
        listAutomationRules(projectId),
        listAutomationExecutions(projectId),
      ]);
      setRules(nextRules);
      setExecutions(nextExecutions);
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not load automation rules.",
      );
    }
  }, [projectId]);
  useEffect(() => {
    void Promise.resolve().then(load);
  }, [load]);
  async function save(
    event: FormEvent<HTMLFormElement>,
    rule?: AutomationRule,
  ) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    setBusy(true);
    setError("");
    setMessage("");
    const input = {
      name: String(data.get("name")).trim(),
      enabled: data.get("enabled") === "on",
      targetStatus: String(data.get("targetStatus")) as IssueStatus,
      action: String(data.get("action")) as
        | "NOTIFY_REPORTER"
        | "NOTIFY_ASSIGNEE",
    };
    try {
      if (rule) await updateAutomationRule(projectId, rule.id, input);
      else {
        await createAutomationRule(projectId, input);
        form.reset();
      }
      setMessage(rule ? "Automation rule saved." : "Automation rule created.");
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not save the automation rule.",
      );
    } finally {
      setBusy(false);
    }
  }
  async function remove(rule: AutomationRule) {
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await deleteAutomationRule(projectId, rule.id);
      setMessage("Automation rule deleted. Its execution history is retained.");
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not delete the automation rule.",
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="detail-card automation-settings">
      <div>
        <h2>Automation rules</h2>
        <p className="subtle">
          When an issue enters a status, notify its reporter or assignee.
          Notifications cannot trigger more rules.
        </p>
      </div>
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      {message && (
        <p className="notice" role="status">
          {message}
        </p>
      )}
      <form className="automation-form" onSubmit={(event) => void save(event)}>
        <label>
          Rule name
          <input
            name="name"
            required
            maxLength={100}
            placeholder="Notify reporter when work is done"
          />
        </label>
        <label>
          When status becomes
          <select name="targetStatus" defaultValue="DONE">
            {statuses.map((status) => (
              <option key={status} value={status}>
                {status.replaceAll("_", " ").toLowerCase()}
              </option>
            ))}
          </select>
        </label>
        <label>
          Notify
          <select name="action" defaultValue="NOTIFY_REPORTER">
            <option value="NOTIFY_REPORTER">Reporter</option>
            <option value="NOTIFY_ASSIGNEE">Assignee</option>
          </select>
        </label>
        <label className="automation-enabled">
          <input type="checkbox" name="enabled" defaultChecked /> Enabled
        </label>
        <button className="primary-button compact" disabled={busy}>
          Create rule
        </button>
      </form>
      <div className="automation-rule-list">
        {rules.map((rule) => (
          <form
            className="automation-rule"
            key={rule.id}
            onSubmit={(event) => void save(event, rule)}
          >
            <label>
              Rule name
              <input
                name="name"
                required
                maxLength={100}
                defaultValue={rule.name}
                key={`${rule.id}-${rule.updatedAt}`}
              />
            </label>
            <label>
              Status
              <select name="targetStatus" defaultValue={rule.targetStatus}>
                {statuses.map((status) => (
                  <option key={status} value={status}>
                    {status.replaceAll("_", " ").toLowerCase()}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Notify
              <select name="action" defaultValue={rule.action}>
                <option value="NOTIFY_REPORTER">Reporter</option>
                <option value="NOTIFY_ASSIGNEE">Assignee</option>
              </select>
            </label>
            <label className="automation-enabled">
              <input
                type="checkbox"
                name="enabled"
                defaultChecked={rule.enabled}
              />{" "}
              Enabled
            </label>
            <div className="automation-actions">
              <button className="secondary-button compact" disabled={busy}>
                Save
              </button>
              <button
                type="button"
                className="text-action"
                disabled={busy}
                onClick={() => void remove(rule)}
              >
                Delete
              </button>
            </div>
          </form>
        ))}
      </div>
      <div className="automation-history">
        <h3>Recent executions</h3>
        {executions.length ? (
          <div className="report-table-wrap">
            <table className="report-table">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Rule</th>
                  <th>Issue</th>
                  <th>Result</th>
                  <th>Details</th>
                </tr>
              </thead>
              <tbody>
                {executions.map((item) => (
                  <tr key={item.id}>
                    <td>{new Date(item.executedAt).toLocaleString()}</td>
                    <td>{item.ruleName}</td>
                    <td>{item.issueKey}</td>
                    <td>{item.outcome.toLowerCase()}</td>
                    <td>{item.detail}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="subtle">Rule runs will appear here.</p>
        )}
      </div>
    </section>
  );
}

function WorkflowSettings({ projectId }: { projectId: string }) {
  const statuses: IssueStatus[] = ["TODO", "IN_PROGRESS", "IN_REVIEW", "DONE"];
  const [allowed, setAllowed] = useState<WorkflowTransition[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  useEffect(() => {
    void getWorkflow(projectId)
      .then((workflow) => {
        setAllowed(workflow.transitions);
        setLoaded(true);
      })
      .catch((e: unknown) => {
        setMessage(
          e instanceof ApiError ? e.message : "Could not load workflow.",
        );
        setLoaded(true);
      });
  }, [projectId]);
  function toggle(from: IssueStatus, to: IssueStatus) {
    setAllowed((current) =>
      current.some((item) => item.from === from && item.to === to)
        ? current.filter((item) => item.from !== from || item.to !== to)
        : [...current, { from, to }],
    );
  }
  async function saveWorkflow() {
    setBusy(true);
    setMessage("");
    try {
      const result = await updateWorkflow(projectId, allowed);
      setAllowed(result.transitions);
      setMessage("Workflow saved.");
    } catch (e) {
      setMessage(
        e instanceof ApiError ? e.message : "Could not save workflow.",
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="detail-card workflow-settings">
      <h2>Workflow transitions</h2>
      <p className="subtle">
        Choose which status changes are allowed. These rules are enforced by the
        server for issue edits and board moves.
      </p>
      {message && (
        <p
          className={message.includes("saved") ? "notice" : "form-error"}
          role="status"
        >
          {message}
        </p>
      )}
      {!loaded ? (
        <p className="subtle">Loading workflow…</p>
      ) : (
        <div className="workflow-grid">
          {statuses.map((from) => (
            <fieldset key={from}>
              <legend>From {from.replace("_", " ")}</legend>
              {statuses
                .filter((to) => to !== from)
                .map((to) => (
                  <label key={to}>
                    <input
                      type="checkbox"
                      checked={allowed.some(
                        (item) => item.from === from && item.to === to,
                      )}
                      onChange={() => toggle(from, to)}
                    />{" "}
                    To {to.replace("_", " ")}
                  </label>
                ))}
            </fieldset>
          ))}
        </div>
      )}
      <button
        className="primary-button compact"
        disabled={!loaded || busy}
        onClick={() => void saveWorkflow()}
      >
        {busy ? "Saving…" : "Save workflow"}
      </button>
    </section>
  );
}

function useProjectContext() {
  return useOutletContext<{
    project: Project;
    members: WorkspaceMember[];
    canManage: boolean;
    setProject: (project: Project) => void;
  }>();
}
