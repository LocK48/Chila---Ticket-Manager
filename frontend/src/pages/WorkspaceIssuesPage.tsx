import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  ApiError,
  listProjects,
  listWorkspaceMembers,
  type Project,
  type WorkspaceMember,
} from "../auth/api";
import { IssuesPanel } from "../features/issues/IssuesPanel";
import { PageHeader } from "../components/ui/PageHeader";
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from "../components/ui/StateMessage";
import { useAuth } from "../auth/useAuth";
import { useAppLayout } from "../layouts/app-layout-context";

export function WorkspaceIssuesPage({ mine = false }: { mine?: boolean }) {
  const { activeWorkspace } = useAppLayout();
  const { user } = useAuth();
  const workspaceId = activeWorkspace?.id ?? "";
  const [projects, setProjects] = useState<Project[]>([]);
  const [members, setMembers] = useState<WorkspaceMember[]>([]);
  const [selectedProject, setSelectedProject] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const canManage = activeWorkspace?.role !== "MEMBER";
  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [projectItems, memberItems] = await Promise.all([
        listProjects(workspaceId),
        listWorkspaceMembers(workspaceId),
      ]);
      setProjects(projectItems);
      setMembers(memberItems);
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not load workspace issues.",
      );
    } finally {
      setLoading(false);
    }
  }, [workspaceId]);
  useEffect(() => {
    void Promise.resolve().then(load);
  }, [load]);
  const shown = useMemo(
    () =>
      projects.filter(
        (project) =>
          selectedProject === "ALL" || project.id === selectedProject,
      ),
    [projects, selectedProject],
  );
  if (loading) return <LoadingState>Loading issues…</LoadingState>;
  return (
    <>
      <PageHeader
        eyebrow={mine ? "PERSONAL WORK" : "WORKSPACE"}
        title={mine ? "My issues" : "All issues"}
        description={
          mine
            ? "Issues assigned to your signed-in account."
            : "Browse issues across projects in this workspace."
        }
        action={
          !mine && shown.length > 0 ? (
            <Link
              className="primary-button compact"
              to={`/workspaces/${workspaceId}/projects/${shown[0].id}`}
            >
              New issue
            </Link>
          ) : undefined
        }
      />
      {error && <ErrorState message={error} onRetry={() => void load()} />}
      {projects.length > 1 && (
        <div className="page-toolbar">
          <label>
            Project
            <select
              aria-label="Filter by project"
              value={selectedProject}
              onChange={(event) => setSelectedProject(event.target.value)}
            >
              <option value="ALL">All projects</option>
              {projects.map((project) => (
                <option key={project.id} value={project.id}>
                  {project.name} ({project.key})
                </option>
              ))}
            </select>
          </label>
        </div>
      )}
      {!projects.length ? (
        <EmptyState
          title="No projects available"
          detail="Issues appear here when this workspace has projects."
        />
      ) : (
        <div className="workspace-issue-groups">
          {shown.map((project) => (
            <section className="workspace-issue-group" key={project.id}>
              <h2>
                <Link to={`/workspaces/${workspaceId}/projects/${project.id}`}>
                  {project.name}
                </Link>
                <span>{project.key}</span>
              </h2>
              <IssuesPanel
                projectId={project.id}
                members={members}
                canManage={canManage}
                active
                projectActive={project.status === "ACTIVE"}
                fixedAssigneeId={mine ? user?.id : undefined}
              />
            </section>
          ))}
        </div>
      )}
    </>
  );
}
