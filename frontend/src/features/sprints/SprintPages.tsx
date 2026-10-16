import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import {
  addIssuesToSprint,
  ApiError,
  assignIssueToSprint,
  completeSprint,
  createIssue,
  createSprint,
  listBacklog,
  listProjectSprints,
  listSprintIssuesAll,
  removeIssuesFromSprint,
  startSprint,
  updateSprint,
  type Issue,
  type IssueInput,
  type IssuePage,
  type IssuePriority,
  type IssueType,
  type Project,
  type Sprint,
  type SprintInput,
  type WorkspaceMember,
} from "../../auth/api";
import { ErrorState, LoadingState } from "../../components/ui/StateMessage";

const emptySprint: SprintInput = {
  name: "",
  goal: null,
  startDate: null,
  endDate: null,
};
const emptyIssue: IssueInput = {
  title: "",
  description: null,
  type: "TASK",
  status: "TODO",
  priority: "MEDIUM",
  assigneeId: null,
  dueDate: null,
};

export function BacklogView({
  project,
  members,
  canManage,
  projectActive,
}: {
  project: Project;
  members: WorkspaceMember[];
  canManage: boolean;
  projectActive: boolean;
}) {
  const { workspaceId = "" } = useParams();
  const [pageData, setPageData] = useState<IssuePage | null>(null);
  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [selected, setSelected] = useState<string[]>([]);
  const [selectedSprint, setSelectedSprint] = useState("");
  const [expandedSprint, setExpandedSprint] = useState("");
  const [sprintIssues, setSprintIssues] = useState<Issue[]>([]);
  const [selectedSprintIssues, setSelectedSprintIssues] = useState<string[]>(
    [],
  );
  const [filter, setFilter] = useState("");
  const [assigneeFilter, setAssigneeFilter] = useState("");
  const [priorityFilter, setPriorityFilter] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  const [editing, setEditing] = useState<Sprint | null>(null);
  const [sprintForm, setSprintForm] = useState<SprintInput>(emptySprint);
  const [showSprintForm, setShowSprintForm] = useState(false);
  const [showIssueForm, setShowIssueForm] = useState(false);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [issues, projectSprints] = await Promise.all([
        listBacklog(project.id, {
          page: 0,
          size: 50,
          search: filter.trim() || undefined,
          assigneeId:
            assigneeFilter && assigneeFilter !== "unassigned"
              ? assigneeFilter
              : undefined,
          unassigned: assigneeFilter === "unassigned" || undefined,
          priority: priorityFilter
            ? (priorityFilter as IssuePriority)
            : undefined,
          type: typeFilter ? (typeFilter as IssueType) : undefined,
        }),
        listProjectSprints(project.id),
      ]);
      setPageData(issues);
      setSprints(projectSprints);
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not load the backlog.",
      );
    } finally {
      setLoading(false);
    }
  }, [filter, assigneeFilter, priorityFilter, typeFilter, project.id]);
  useEffect(() => {
    void Promise.resolve().then(load);
  }, [load]);

  async function planSelected() {
    if (!projectActive || !selectedSprint || !selected.length) return;
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await addIssuesToSprint(project.id, selectedSprint, selected);
      setSelected([]);
      setNotice(
        `${selected.length} issue${selected.length === 1 ? "" : "s"} added to the sprint.`,
      );
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not plan the selected issues.",
      );
    } finally {
      setBusy(false);
    }
  }

  async function move(issue: Issue, beforeIssueId: string | null) {
    setBusy(true);
    setError("");
    try {
      await assignIssueToSprint(issue.id, null, beforeIssueId);
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not reorder the backlog.",
      );
    } finally {
      setBusy(false);
    }
  }

  async function saveSprint(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const data = new FormData(event.currentTarget);
      const input: SprintInput = {
        name: String(data.get("name")).trim(),
        goal: String(data.get("goal") ?? "").trim() || null,
        startDate: String(data.get("startDate") ?? "") || null,
        endDate: String(data.get("endDate") ?? "") || null,
      };
      if (editing) await updateSprint(project.id, editing.id, input);
      else await createSprint(project.id, input);
      setEditing(null);
      setShowSprintForm(false);
      setSprintForm(emptySprint);
      setNotice(editing ? "Sprint updated." : "Sprint created.");
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not save the sprint.",
      );
    } finally {
      setBusy(false);
    }
  }

  async function saveIssue(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const data = new FormData(event.currentTarget);
      await createIssue(project.id, {
        ...emptyIssue,
        title: String(data.get("title")).trim(),
        description: String(data.get("description") ?? "").trim() || null,
        type: String(data.get("type")) as IssueType,
        priority: String(data.get("priority")) as IssuePriority,
        assigneeId: String(data.get("assigneeId") ?? "") || null,
        dueDate: String(data.get("dueDate") ?? "") || null,
      });
      setShowIssueForm(false);
      setNotice("Issue created in the backlog.");
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not create the issue.",
      );
    } finally {
      setBusy(false);
    }
  }

  async function start(sprint: Sprint) {
    setBusy(true);
    setError("");
    try {
      await startSprint(project.id, sprint.id);
      setNotice(`${sprint.name} started.`);
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not start the sprint.",
      );
    } finally {
      setBusy(false);
    }
  }

  async function toggleSprintIssues(sprint: Sprint) {
    if (expandedSprint === sprint.id) {
      setExpandedSprint("");
      setSelectedSprintIssues([]);
      return;
    }
    setBusy(true);
    setError("");
    try {
      setSprintIssues(await listSprintIssuesAll(project.id, sprint.id));
      setExpandedSprint(sprint.id);
      setSelectedSprintIssues([]);
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not load sprint issues.",
      );
    } finally {
      setBusy(false);
    }
  }

  async function removePlannedIssues(sprint: Sprint) {
    if (!selectedSprintIssues.length) return;
    setBusy(true);
    setError("");
    try {
      await removeIssuesFromSprint(project.id, sprint.id, selectedSprintIssues);
      setSelectedSprintIssues([]);
      setSprintIssues(await listSprintIssuesAll(project.id, sprint.id));
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not return these issues to the backlog.",
      );
    } finally {
      setBusy(false);
    }
  }

  const futureSprints = sprints.filter((sprint) => sprint.state === "FUTURE");
  if (loading && !pageData)
    return <LoadingState>Loading backlog…</LoadingState>;
  return (
    <section className="scrum-page">
      <header className="scrum-heading">
        <div>
          <p className="eyebrow">{project.key} · Scrum</p>
          <h2>Product backlog</h2>
          <p className="subtle">
            Prioritize unplanned issues, then move a selection into a future
            sprint.
          </p>
        </div>
        {projectActive && (
          <button
            className="primary-button compact"
            disabled={busy}
            onClick={() => setShowIssueForm((open) => !open)}
          >
            Create issue
          </button>
        )}
      </header>
      {error && <ErrorState message={error} onRetry={() => void load()} />}
      {notice && (
        <p className="notice" role="status">
          {notice}
        </p>
      )}
      {!projectActive && (
        <p className="member-note">
          This project is archived. Backlog and sprint actions are read-only.
        </p>
      )}
      {showIssueForm && (
        <form
          className="board-create-form"
          onSubmit={(event) => void saveIssue(event)}
        >
          <label>
            Title
            <input name="title" required maxLength={200} autoFocus />
          </label>
          <label>
            Description
            <textarea name="description" rows={2} />
          </label>
          <div className="board-create-fields">
            <label>
              Type
              <select name="type" defaultValue="TASK">
                {["TASK", "BUG", "STORY"].map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </label>
            <label>
              Priority
              <select name="priority" defaultValue="MEDIUM">
                {["LOW", "MEDIUM", "HIGH", "URGENT"].map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </label>
            <label>
              Assignee
              <select name="assigneeId" defaultValue="">
                <option value="">Unassigned</option>
                {members.map((member) => (
                  <option key={member.userId} value={member.userId}>
                    {member.displayName}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Due date
              <input name="dueDate" type="date" />
            </label>
          </div>
          <div className="button-row">
            <button className="primary-button compact" disabled={busy}>
              Create in backlog
            </button>
            <button
              type="button"
              className="text-action"
              onClick={() => setShowIssueForm(false)}
            >
              Cancel
            </button>
          </div>
        </form>
      )}

      <section className="sprint-planning-panel">
        <div className="scrum-section-heading">
          <div>
            <h3>Sprints</h3>
            <p className="subtle">
              Future plans and closed sprint history stay with this project.
            </p>
          </div>
          {canManage && projectActive && (
            <button
              className="secondary-button"
              onClick={() => {
                setEditing(null);
                setSprintForm(emptySprint);
                setShowSprintForm((open) => !open);
              }}
            >
              Create sprint
            </button>
          )}
        </div>
        {showSprintForm && (
          <form
            className="board-create-form"
            onSubmit={(event) => void saveSprint(event)}
          >
            <label>
              Sprint name
              <input
                name="name"
                required
                maxLength={120}
                value={sprintForm.name}
                onChange={(event) =>
                  setSprintForm({ ...sprintForm, name: event.target.value })
                }
              />
            </label>
            <label>
              Goal
              <textarea
                name="goal"
                rows={2}
                value={sprintForm.goal ?? ""}
                onChange={(event) =>
                  setSprintForm({
                    ...sprintForm,
                    goal: event.target.value || null,
                  })
                }
              />
            </label>
            <div className="board-create-fields">
              <label>
                Planned start
                <input
                  name="startDate"
                  type="date"
                  value={sprintForm.startDate ?? ""}
                  onChange={(event) =>
                    setSprintForm({
                      ...sprintForm,
                      startDate: event.target.value || null,
                    })
                  }
                />
              </label>
              <label>
                Planned end
                <input
                  name="endDate"
                  type="date"
                  value={sprintForm.endDate ?? ""}
                  onChange={(event) =>
                    setSprintForm({
                      ...sprintForm,
                      endDate: event.target.value || null,
                    })
                  }
                />
              </label>
            </div>
            <div className="button-row">
              <button className="primary-button compact" disabled={busy}>
                {editing ? "Save sprint" : "Create sprint"}
              </button>
              <button
                type="button"
                className="text-action"
                onClick={() => {
                  setShowSprintForm(false);
                  setEditing(null);
                }}
              >
                Cancel
              </button>
            </div>
          </form>
        )}
        {!sprints.length && <p className="kanban-empty">No sprints yet.</p>}
        <div className="sprint-list">
          {sprints.map((sprint) => (
            <article className="sprint-summary" key={sprint.id}>
              <div className="sprint-summary-main">
                <div className="kanban-card-meta">
                  <strong>{sprint.name}</strong>
                  <span
                    className={`sprint-state sprint-state-${sprint.state.toLowerCase()}`}
                  >
                    {sprint.state.toLowerCase()}
                  </span>
                </div>
                <p>{sprint.goal || "No sprint goal."}</p>
                <small>
                  {sprint.startDate || "Start date TBD"} –{" "}
                  {sprint.endDate || "End date TBD"}
                </small>
              </div>
              <div className="sprint-summary-actions">
                {sprint.state === "ACTIVE" && (
                  <Link
                    className="secondary-button compact"
                    to={`/workspaces/${workspaceId}/projects/${project.id}/active-sprint`}
                  >
                    Open sprint
                  </Link>
                )}
                {canManage && projectActive && sprint.state === "FUTURE" && (
                  <>
                    <button
                      className="text-action"
                      disabled={busy}
                      onClick={() => {
                        setEditing(sprint);
                        setSprintForm({
                          name: sprint.name,
                          goal: sprint.goal,
                          startDate: sprint.startDate,
                          endDate: sprint.endDate,
                        });
                        setShowSprintForm(true);
                      }}
                    >
                      Edit
                    </button>
                    <button
                      className="secondary-button compact"
                      disabled={
                        busy ||
                        sprints.some(
                          (candidate) => candidate.state === "ACTIVE",
                        )
                      }
                      onClick={() => void start(sprint)}
                    >
                      Start sprint
                    </button>
                  </>
                )}
                {sprint.state !== "ACTIVE" && (
                  <button
                    className="text-action"
                    disabled={busy}
                    onClick={() => void toggleSprintIssues(sprint)}
                  >
                    {expandedSprint === sprint.id
                      ? "Hide issues"
                      : sprint.state === "CLOSED"
                        ? "View history"
                        : "View planned issues"}
                  </button>
                )}
              </div>
              {expandedSprint === sprint.id && (
                <div className="sprint-history-issues">
                  {sprintIssues.length === 0 && (
                    <p className="kanban-empty">
                      No issues are recorded in this sprint.
                    </p>
                  )}
                  {sprintIssues.map((issue) => (
                    <label key={issue.id}>
                      <input
                        type="checkbox"
                        checked={selectedSprintIssues.includes(issue.id)}
                        disabled={sprint.state === "CLOSED" || !projectActive}
                        onChange={(event) =>
                          setSelectedSprintIssues((current) =>
                            event.target.checked
                              ? [...current, issue.id]
                              : current.filter((id) => id !== issue.id),
                          )
                        }
                      />
                      <Link
                        to={`/workspaces/${workspaceId}/issues/${issue.issueKey}`}
                      >
                        {issue.issueKey}
                      </Link>
                      <span>{issue.title}</span>
                      <span>
                        {issue.status.replace("_", " ").toLowerCase()}
                      </span>
                    </label>
                  ))}
                  {sprint.state !== "CLOSED" &&
                    projectActive &&
                    selectedSprintIssues.length > 0 && (
                      <button
                        className="text-action"
                        disabled={busy}
                        onClick={() => void removePlannedIssues(sprint)}
                      >
                        Return selected to backlog
                      </button>
                    )}
                </div>
              )}
            </article>
          ))}
        </div>
      </section>

      <section className="backlog-panel">
        <div className="scrum-section-heading">
          <div>
            <h3>
              Backlog issues{" "}
              <span className="issue-count">{pageData?.totalItems ?? 0}</span>
            </h3>
            <p className="subtle">
              Ordering is saved and separate from workflow status.
            </p>
          </div>
          <div className="backlog-filters">
            <input
              className="backlog-search"
              aria-label="Search backlog"
              placeholder="Search by key or title"
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
            />
            <select
              aria-label="Filter backlog by assignee"
              value={assigneeFilter}
              onChange={(event) => setAssigneeFilter(event.target.value)}
            >
              <option value="">Any assignee</option>
              <option value="unassigned">Unassigned</option>
              {members.map((member) => (
                <option key={member.userId} value={member.userId}>
                  {member.displayName}
                </option>
              ))}
            </select>
            <select
              aria-label="Filter backlog by priority"
              value={priorityFilter}
              onChange={(event) => setPriorityFilter(event.target.value)}
            >
              <option value="">Any priority</option>
              {["LOW", "MEDIUM", "HIGH", "URGENT"].map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
            <select
              aria-label="Filter backlog by type"
              value={typeFilter}
              onChange={(event) => setTypeFilter(event.target.value)}
            >
              <option value="">Any type</option>
              {["TASK", "BUG", "STORY"].map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
          </div>
        </div>
        {selected.length > 0 && (
          <div className="backlog-selection">
            <span>{selected.length} selected</span>
            <select
              aria-label="Select future sprint"
              value={selectedSprint}
              onChange={(event) => setSelectedSprint(event.target.value)}
            >
              <option value="">Choose a future sprint</option>
              {futureSprints.map((sprint) => (
                <option key={sprint.id} value={sprint.id}>
                  {sprint.name}
                </option>
              ))}
            </select>
            <button
              className="primary-button compact"
              disabled={busy || !projectActive || !selectedSprint}
              onClick={() => void planSelected()}
            >
              Add to sprint
            </button>
          </div>
        )}
        {!pageData?.items.length && (
          <p className="kanban-empty">
            {filter
              ? "No matching backlog issues."
              : "Backlog is clear. Create an issue or move unfinished work here."}
          </p>
        )}
        <div className="backlog-issue-list">
          {pageData?.items.map((issue, index) => (
            <BacklogIssue
              key={issue.id}
              issue={issue}
              previousId={pageData.items[index - 1]?.id ?? null}
              nextAfterId={pageData.items[index + 2]?.id ?? null}
              isLast={
                index === pageData.items.length - 1 &&
                pageData.page + 1 === pageData.totalPages
              }
              selected={selected.includes(issue.id)}
              disabled={busy || !projectActive}
              onSelect={(checked) =>
                setSelected((current) =>
                  checked
                    ? [...current, issue.id]
                    : current.filter((id) => id !== issue.id),
                )
              }
              onMove={(beforeId) => void move(issue, beforeId)}
              workspaceId={workspaceId}
            />
          ))}
        </div>
        {pageData && pageData.page + 1 < pageData.totalPages && (
          <button
            className="kanban-load-more"
            disabled={loading}
            onClick={() =>
              void listBacklog(project.id, {
                page: pageData.page + 1,
                size: 50,
                search: filter.trim() || undefined,
                assigneeId:
                  assigneeFilter && assigneeFilter !== "unassigned"
                    ? assigneeFilter
                    : undefined,
                unassigned: assigneeFilter === "unassigned" || undefined,
                priority: priorityFilter
                  ? (priorityFilter as IssuePriority)
                  : undefined,
                type: typeFilter ? (typeFilter as IssueType) : undefined,
              })
                .then((next) =>
                  setPageData((current) =>
                    current
                      ? { ...next, items: [...current.items, ...next.items] }
                      : next,
                  ),
                )
                .catch((cause) =>
                  setError(
                    cause instanceof ApiError
                      ? cause.message
                      : "Could not load more backlog issues.",
                  ),
                )
            }
          >
            Load more
          </button>
        )}
      </section>
    </section>
  );
}

function BacklogIssue({
  issue,
  previousId,
  nextAfterId,
  isLast,
  selected,
  disabled,
  onSelect,
  onMove,
  workspaceId,
}: {
  issue: Issue;
  previousId: string | null;
  nextAfterId: string | null;
  isLast: boolean;
  selected: boolean;
  disabled: boolean;
  onSelect: (checked: boolean) => void;
  onMove: (beforeId: string | null) => void;
  workspaceId: string;
}) {
  return (
    <article className="backlog-issue-row">
      <input
        type="checkbox"
        aria-label={`Select ${issue.issueKey}`}
        checked={selected}
        onChange={(event) => onSelect(event.target.checked)}
        disabled={disabled}
      />
      <div className="backlog-rank-controls">
        <button
          aria-label={`Move ${issue.issueKey} up`}
          disabled={disabled || !previousId}
          onClick={() => previousId && onMove(previousId)}
        >
          ↑
        </button>
        <button
          aria-label={`Move ${issue.issueKey} down`}
          disabled={disabled || isLast}
          onClick={() => onMove(nextAfterId)}
        >
          ↓
        </button>
      </div>
      <div className="backlog-issue-main">
        <div className="kanban-card-meta">
          <Link to={`/workspaces/${workspaceId}/issues/${issue.issueKey}`}>
            {issue.issueKey}
          </Link>
          <span>{issue.type.toLowerCase()}</span>
        </div>
        <Link
          className="kanban-card-title"
          to={`/workspaces/${workspaceId}/issues/${issue.issueKey}`}
        >
          {issue.title}
        </Link>
      </div>
      <span
        className={`priority-label priority-${issue.priority.toLowerCase()}`}
      >
        {issue.priority.toLowerCase()}
      </span>
      <span className="backlog-assignee">
        {issue.assigneeName ?? "Unassigned"}
      </span>
    </article>
  );
}

export function ActiveSprintView({
  project,
  members,
  canManage,
  projectActive,
}: {
  project: Project;
  members: WorkspaceMember[];
  canManage: boolean;
  projectActive: boolean;
}) {
  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [issues, setIssues] = useState<Issue[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [action, setAction] = useState<"BACKLOG" | "FUTURE_SPRINT">("BACKLOG");
  const [targetSprintId, setTargetSprintId] = useState("");
  const [selected, setSelected] = useState<string[]>([]);
  const active = sprints.find((sprint) => sprint.state === "ACTIVE");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await listProjectSprints(project.id);
      setSprints(data);
      const running = data.find((sprint) => sprint.state === "ACTIVE");
      setIssues(
        running ? await listSprintIssuesAll(project.id, running.id) : [],
      );
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not load the active sprint.",
      );
    } finally {
      setLoading(false);
    }
  }, [project.id]);
  useEffect(() => {
    void Promise.resolve().then(load);
  }, [load]);

  async function addFromBacklog(issueIds: string[]) {
    if (!active || !issueIds.length) return;
    setBusy(true);
    setError("");
    try {
      await addIssuesToSprint(project.id, active.id, issueIds);
      setSelected([]);
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not add issues to the sprint.",
      );
    } finally {
      setBusy(false);
    }
  }
  async function finish() {
    if (!active) return;
    setBusy(true);
    setError("");
    try {
      await completeSprint(
        project.id,
        active.id,
        action,
        action === "FUTURE_SPRINT" ? targetSprintId : null,
      );
      setTargetSprintId("");
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not complete this sprint.",
      );
    } finally {
      setBusy(false);
    }
  }
  async function removeSelected() {
    if (!active || !selected.length) return;
    setBusy(true);
    setError("");
    try {
      await removeIssuesFromSprint(project.id, active.id, selected);
      setSelected([]);
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not return issues to the backlog.",
      );
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <LoadingState>Loading active sprint…</LoadingState>;
  if (error && !sprints.length)
    return <ErrorState message={error} onRetry={() => void load()} />;
  const doneCount = issues.filter((issue) => issue.status === "DONE").length;
  const futureSprints = sprints.filter((sprint) => sprint.state === "FUTURE");
  return (
    <section className="scrum-page">
      <header className="scrum-heading">
        <div>
          <p className="eyebrow">{project.key} · Scrum</p>
          <h2>{active?.name ?? "Active sprint"}</h2>
          <p className="subtle">
            {active?.goal ||
              (active
                ? "No sprint goal."
                : "There is no active sprint right now.")}
          </p>
          {active && (
            <small>
              {active.startDate} – {active.endDate} · {doneCount} of{" "}
              {issues.length} issues complete
            </small>
          )}
        </div>
      </header>
      {error && <ErrorState message={error} onRetry={() => void load()} />}
      {!projectActive && (
        <p className="member-note">
          This project is archived. The sprint is read-only.
        </p>
      )}
      {!active ? (
        <div className="detail-card">
          <h3>No active sprint</h3>
          <p>
            Choose a planned sprint from the backlog and start it when planning
            is complete.
          </p>
          <Link
            className="secondary-button"
            to={`/workspaces/${project.workspaceId}/projects/${project.id}/backlog`}
          >
            Go to backlog
          </Link>
        </div>
      ) : (
        <>
          {canManage && projectActive && (
            <section className="sprint-completion-panel">
              <label>
                Unfinished issues
                <select
                  value={action}
                  onChange={(event) =>
                    setAction(event.target.value as typeof action)
                  }
                >
                  <option value="BACKLOG">Return them to the backlog</option>
                  <option value="FUTURE_SPRINT">
                    Move them to a future sprint
                  </option>
                </select>
              </label>
              {action === "FUTURE_SPRINT" && (
                <label>
                  Future sprint
                  <select
                    value={targetSprintId}
                    onChange={(event) => setTargetSprintId(event.target.value)}
                  >
                    <option value="">Choose a sprint</option>
                    {futureSprints.map((sprint) => (
                      <option key={sprint.id} value={sprint.id}>
                        {sprint.name}
                      </option>
                    ))}
                  </select>
                </label>
              )}
              <button
                className="secondary-button"
                disabled={
                  busy || (action === "FUTURE_SPRINT" && !targetSprintId)
                }
                onClick={() => void finish()}
              >
                Complete sprint
              </button>
            </section>
          )}
          {projectActive && issues.length > 0 && (
            <div className="backlog-selection">
              <label>
                <input
                  type="checkbox"
                  checked={
                    selected.length > 0 && selected.length === issues.length
                  }
                  onChange={(event) =>
                    setSelected(
                      event.target.checked
                        ? issues.map((issue) => issue.id)
                        : [],
                    )
                  }
                />{" "}
                Select sprint issues
              </label>
              <button
                className="text-action"
                disabled={busy || !selected.length}
                onClick={() => void removeSelected()}
              >
                Return selection to backlog
              </button>
            </div>
          )}
          <KanbanSprintBoard
            projectId={project.id}
            sprintId={active.id}
            members={members}
            projectActive={projectActive}
            issues={issues}
            selected={selected}
            onSelect={(id, checked) =>
              setSelected((current) =>
                checked
                  ? [...current, id]
                  : current.filter((item) => item !== id),
              )
            }
          />
          {projectActive && (
            <section className="backlog-selection">
              <Link
                className="secondary-button"
                to={`/workspaces/${project.workspaceId}/projects/${project.id}/backlog`}
              >
                Open backlog
              </Link>
              <BacklogPlanner
                project={project}
                sprint={active}
                disabled={busy}
                onAdd={(ids) => void addFromBacklog(ids)}
              />
            </section>
          )}
        </>
      )}
    </section>
  );
}

import { KanbanBoard } from "../issues/KanbanBoard";

function KanbanSprintBoard({
  projectId,
  sprintId,
  members,
  projectActive,
  issues,
  selected,
  onSelect,
}: {
  projectId: string;
  sprintId: string;
  members: WorkspaceMember[];
  projectActive: boolean;
  issues: Issue[];
  selected: string[];
  onSelect: (id: string, checked: boolean) => void;
}) {
  return (
    <>
      <div className="sprint-issue-selection">
        {issues.map((issue) => (
          <label key={issue.id}>
            <input
              type="checkbox"
              aria-label={`Select ${issue.issueKey}`}
              checked={selected.includes(issue.id)}
              onChange={(event) => onSelect(issue.id, event.target.checked)}
            />
            {issue.issueKey}
          </label>
        ))}
      </div>
      <KanbanBoard
        projectId={projectId}
        sprintId={sprintId}
        members={members}
        projectActive={projectActive}
      />
    </>
  );
}

function BacklogPlanner({
  project,
  sprint,
  disabled,
  onAdd,
}: {
  project: Project;
  sprint: Sprint;
  disabled: boolean;
  onAdd: (ids: string[]) => void;
}) {
  const [ids, setIds] = useState<string[]>([]);
  const [items, setItems] = useState<Issue[]>([]);
  useEffect(() => {
    void listBacklog(project.id, { size: 100 })
      .then((page) => setItems(page.items))
      .catch(() => setItems([]));
  }, [project.id]);
  return (
    <div className="backlog-planner">
      <label>
        Add backlog issues
        <select
          aria-label="Backlog issues to add"
          multiple
          value={ids}
          onChange={(event) =>
            setIds(
              Array.from(
                event.target.selectedOptions,
                (option) => option.value,
              ),
            )
          }
        >
          {items.map((issue) => (
            <option key={issue.id} value={issue.id}>
              {issue.issueKey} · {issue.title}
            </option>
          ))}
        </select>
      </label>
      <button
        className="primary-button compact"
        disabled={disabled || !ids.length}
        onClick={() => {
          onAdd(ids);
          setIds([]);
        }}
      >
        Add to {sprint.name}
      </button>
    </div>
  );
}
