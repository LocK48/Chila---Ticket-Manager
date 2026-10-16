import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import {
  ApiError,
  archiveIssue,
  createIssue,
  listIssues,
  restoreIssue,
  updateIssue,
  createSavedFilter,
  deleteSavedFilter,
  listSavedFilters,
  listProjectSprints,
  listProjectEpics,
  getWorkflow,
  type Issue,
  type IssueInput,
  type IssuePriority,
  type IssueStatus,
  type IssueType,
  type WorkspaceMember,
  type SavedFilter,
  type SavedFilterVisibility,
} from "../../auth/api";
import { IssueCollaboration } from "./IssueCollaboration";
import { useAuth } from "../../auth/useAuth";

const statuses: IssueStatus[] = ["TODO", "IN_PROGRESS", "IN_REVIEW", "DONE"];
const priorities: IssuePriority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];
const blank: IssueInput = {
  title: "",
  description: null,
  type: "TASK",
  status: "TODO",
  priority: "MEDIUM",
  assigneeId: null,
  dueDate: null,
};

export function IssuesPanel({
  projectId,
  members,
  canManage,
  active,
  projectActive,
  fixedAssigneeId,
}: {
  projectId: string;
  members: WorkspaceMember[];
  canManage: boolean;
  active: boolean;
  projectActive: boolean;
  fixedAssigneeId?: string;
}) {
  const { workspaceId = "" } = useParams();
  const { user } = useAuth();
  const [issues, setIssues] = useState<Issue[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [priority, setPriority] = useState("");
  const [type, setType] = useState("");
  const [assigneeId, setAssigneeId] = useState(fixedAssigneeId ?? "");
  const [includeArchived, setIncludeArchived] = useState(false);
  const [sort, setSort] = useState("createdAt");
  const [direction, setDirection] = useState<"asc" | "desc">("desc");
  const [sprintId, setSprintId] = useState("");
  const [epicId, setEpicId] = useState("");
  const [dueFrom, setDueFrom] = useState("");
  const [dueTo, setDueTo] = useState("");
  const [savedFilters, setSavedFilters] = useState<SavedFilter[]>([]);
  const [selectedFilter, setSelectedFilter] = useState("");
  const [sprints, setSprints] = useState<
    Awaited<ReturnType<typeof listProjectSprints>>
  >([]);
  const [epics, setEpics] = useState<
    Awaited<ReturnType<typeof listProjectEpics>>
  >([]);
  const [allowedTransitions, setAllowedTransitions] = useState<
    Record<IssueStatus, IssueStatus[]>
  >({
    TODO: ["IN_PROGRESS"],
    IN_PROGRESS: ["TODO", "IN_REVIEW"],
    IN_REVIEW: ["IN_PROGRESS", "DONE"],
    DONE: ["IN_PROGRESS"],
  });
  const [editing, setEditing] = useState("");
  const [expandedIssueIds, setExpandedIssueIds] = useState<Set<string>>(
    () => new Set(),
  );
  const [draft, setDraft] = useState<IssueInput>(blank);
  const [creating, setCreating] = useState(false);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await listIssues(projectId, {
        page,
        size: 10,
        search: search || undefined,
        status: (status || undefined) as IssueStatus | undefined,
        priority: (priority || undefined) as IssuePriority | undefined,
        type: (type || undefined) as IssueType | undefined,
        assigneeId:
          fixedAssigneeId ??
          (assigneeId && assigneeId !== "unassigned" ? assigneeId : undefined),
        unassigned: fixedAssigneeId
          ? undefined
          : assigneeId === "unassigned" || undefined,
        includeArchived,
        sort,
        direction,
        sprintId: sprintId || undefined,
        epicId: epicId || undefined,
        dueFrom: dueFrom || undefined,
        dueTo: dueTo || undefined,
      });
      setIssues(result.items);
      setTotal(result.totalItems);
      setError("");
    } catch (cause) {
      setError(
        cause instanceof ApiError ? cause.message : "Could not load issues.",
      );
    } finally {
      setLoading(false);
    }
  }, [
    projectId,
    page,
    search,
    status,
    priority,
    type,
    assigneeId,
    fixedAssigneeId,
    includeArchived,
    sort,
    direction,
    sprintId,
    epicId,
    dueFrom,
    dueTo,
  ]);
  useEffect(() => {
    if (active) void Promise.resolve().then(load);
  }, [active, load]);
  useEffect(() => {
    if (!active) return;
    void Promise.all([
      listSavedFilters(projectId),
      listProjectSprints(projectId),
      listProjectEpics(projectId),
      getWorkflow(projectId),
    ])
      .then(([filters, sprintOptions, epicOptions, workflow]) => {
        setSavedFilters(filters);
        setSprints(sprintOptions);
        setEpics(epicOptions);
        const map = Object.fromEntries(
          workflow.statuses.map((from) => [
            from,
            workflow.transitions
              .filter((item) => item.from === from)
              .map((item) => item.to),
          ]),
        ) as Record<IssueStatus, IssueStatus[]>;
        setAllowedTransitions(map);
      })
      .catch(() => {});
  }, [active, projectId]);

  function applyFilter(filter: SavedFilter) {
    setSelectedFilter(filter.id);
    setPage(0);
    setSearch(filter.search ?? "");
    setStatus(filter.status ?? "");
    setPriority(filter.priority ?? "");
    setType(filter.type ?? "");
    setAssigneeId(filter.unassigned ? "unassigned" : (filter.assigneeId ?? ""));
    setSprintId(filter.sprintId ?? "");
    setEpicId(filter.epicId ?? "");
    setDueFrom(filter.dueFrom ?? "");
    setDueTo(filter.dueTo ?? "");
  }
  async function saveCurrentFilter() {
    const name = window.prompt("Name this filter")?.trim();
    if (!name) return;
    const visibility: SavedFilterVisibility = window.confirm(
      "Share this filter with workspace members?",
    )
      ? "WORKSPACE"
      : "PRIVATE";
    try {
      const saved = await createSavedFilter(projectId, {
        name,
        visibility,
        status: (status || null) as IssueStatus | null,
        priority: (priority || null) as IssuePriority | null,
        type: (type || null) as IssueType | null,
        assigneeId:
          assigneeId && assigneeId !== "unassigned" ? assigneeId : null,
        unassigned: assigneeId === "unassigned",
        sprintId: sprintId || null,
        epicId: epicId || null,
        dueFrom: dueFrom || null,
        dueTo: dueTo || null,
        search: search.trim() || null,
      });
      setSavedFilters((current) =>
        [...current.filter((item) => item.id !== saved.id), saved].sort(
          (a, b) => a.name.localeCompare(b.name),
        ),
      );
      setSelectedFilter(saved.id);
      setNotice("Filter saved.");
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not save this filter.",
      );
    }
  }
  async function removeSavedFilter() {
    const filter = savedFilters.find((item) => item.id === selectedFilter);
    if (!filter || filter.creatorId !== user?.id) return;
    try {
      await deleteSavedFilter(projectId, filter.id);
      setSavedFilters((current) =>
        current.filter((item) => item.id !== filter.id),
      );
      setSelectedFilter("");
      setNotice("Filter deleted.");
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not delete this filter.",
      );
    }
  }

  function startEdit(issue: Issue) {
    setCreating(false);
    setEditing(issue.id);
    setDraft({
      title: issue.title,
      description: issue.description,
      type: issue.type,
      status: issue.status,
      priority: issue.priority,
      assigneeId: issue.assigneeId,
      dueDate: issue.dueDate,
    });
  }
  async function save(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      if (creating) await createIssue(projectId, draft);
      else if (editing) await updateIssue(editing, draft);
      setNotice(creating ? "Issue created." : "Issue updated.");
      setCreating(false);
      setEditing("");
      setDraft(blank);
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not save this issue.",
      );
    } finally {
      setBusy(false);
    }
  }
  async function archive(issue: Issue) {
    setBusy(true);
    setError("");
    try {
      await archiveIssue(issue.id);
      setNotice("Issue archived.");
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not archive this issue.",
      );
    } finally {
      setBusy(false);
    }
  }
  async function restore(issue: Issue) {
    setBusy(true);
    setError("");
    try {
      await restoreIssue(issue.id);
      setNotice("Issue restored.");
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not restore this issue.",
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="issues-panel" aria-label="Project issues">
      <div className="issues-heading">
        <div>
          <h5>Issues</h5>
          <p>
            {total} {total === 1 ? "issue" : "issues"}
          </p>
        </div>
        {projectActive && (
          <button
            className="text-action"
            onClick={() => {
              setCreating(true);
              setEditing("");
              setDraft(blank);
            }}
          >
            New issue
          </button>
        )}
      </div>
      <div className="issue-filters">
        <select
          aria-label="Saved filters"
          value={selectedFilter}
          onChange={(event) => {
            const value = event.target.value;
            setSelectedFilter(value);
            const filter = savedFilters.find((item) => item.id === value);
            if (filter) applyFilter(filter);
          }}
        >
          <option value="">Saved filters</option>
          {savedFilters.map((filter) => (
            <option key={filter.id} value={filter.id}>
              {filter.name}
              {filter.visibility === "WORKSPACE" ? " · shared" : ""}
            </option>
          ))}
        </select>
        <button
          className="secondary-button compact"
          type="button"
          onClick={() => void saveCurrentFilter()}
        >
          Save filter
        </button>
        {savedFilters.find((item) => item.id === selectedFilter)?.creatorId ===
          user?.id && (
          <button
            className="text-action"
            type="button"
            onClick={() => void removeSavedFilter()}
          >
            Delete saved filter
          </button>
        )}
        <input
          aria-label="Search issues"
          placeholder="Search issues"
          value={search}
          onChange={(event) => {
            setPage(0);
            setSearch(event.target.value);
          }}
        />
        <select
          aria-label="Filter by status"
          value={status}
          onChange={(event) => {
            setPage(0);
            setStatus(event.target.value);
          }}
        >
          <option value="">All statuses</option>
          {statuses.map((item) => (
            <option key={item}>{item}</option>
          ))}
        </select>
        <select
          aria-label="Filter by priority"
          value={priority}
          onChange={(event) => {
            setPage(0);
            setPriority(event.target.value);
          }}
        >
          <option value="">All priorities</option>
          {priorities.map((item) => (
            <option key={item}>{item}</option>
          ))}
        </select>
        <select
          aria-label="Filter by type"
          value={type}
          onChange={(event) => {
            setPage(0);
            setType(event.target.value);
          }}
        >
          <option value="">All types</option>
          {(["TASK", "BUG", "STORY"] as IssueType[]).map((item) => (
            <option key={item}>{item}</option>
          ))}
        </select>
        <select
          aria-label="Filter by sprint"
          value={sprintId}
          onChange={(event) => {
            setPage(0);
            setSprintId(event.target.value);
          }}
        >
          <option value="">All sprints</option>
          {sprints.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
        <select
          aria-label="Filter by epic"
          value={epicId}
          onChange={(event) => {
            setPage(0);
            setEpicId(event.target.value);
          }}
        >
          <option value="">All epics</option>
          {epics.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
        <div className="issue-due-range">
          <label>
            Due from
            <input
              aria-label="Due from"
              type="date"
              value={dueFrom}
              onChange={(event) => {
                setPage(0);
                setDueFrom(event.target.value);
              }}
            />
          </label>
          <label>
            Due to
            <input
              aria-label="Due to"
              type="date"
              value={dueTo}
              onChange={(event) => {
                setPage(0);
                setDueTo(event.target.value);
              }}
            />
          </label>
        </div>
        {!fixedAssigneeId && (
          <select
            aria-label="Filter by assignee"
            value={assigneeId}
            onChange={(event) => {
              setPage(0);
              setAssigneeId(event.target.value);
            }}
          >
            <option value="">Anyone</option>
            <option value="unassigned">Unassigned</option>
            {members.map((member) => (
              <option key={member.userId} value={member.userId}>
                {member.displayName}
              </option>
            ))}
          </select>
        )}
        <select
          aria-label="Sort issues by"
          value={sort}
          onChange={(event) => setSort(event.target.value)}
        >
          <option value="createdAt">Created</option>
          <option value="dueDate">Due date</option>
          <option value="priority">Priority</option>
          <option value="status">Status</option>
          <option value="title">Title</option>
        </select>
        <select
          aria-label="Sort direction"
          value={direction}
          onChange={(event) =>
            setDirection(event.target.value as "asc" | "desc")
          }
        >
          <option value="desc">Descending</option>
          <option value="asc">Ascending</option>
        </select>
        {canManage && (
          <label className="issue-archived-toggle">
            <input
              type="checkbox"
              checked={includeArchived}
              onChange={(event) => setIncludeArchived(event.target.checked)}
            />{" "}
            Archived
          </label>
        )}
      </div>
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      {notice && (
        <p className="notice issue-notice" role="status">
          {notice}
          <button aria-label="Dismiss message" onClick={() => setNotice("")}>
            Dismiss
          </button>
        </p>
      )}
      {!projectActive && (
        <p className="member-note">
          This project is archived. Issues are read-only until the project is
          active again.
        </p>
      )}
      {(creating || editing) && (
        <form className="issue-form" onSubmit={(event) => void save(event)}>
          <label>
            Title
            <input
              required
              maxLength={200}
              value={draft.title}
              onChange={(event) =>
                setDraft({ ...draft, title: event.target.value })
              }
            />
          </label>
          <label>
            Description
            <textarea
              rows={2}
              maxLength={10000}
              value={draft.description ?? ""}
              onChange={(event) =>
                setDraft({ ...draft, description: event.target.value || null })
              }
            />
          </label>
          <div className="issue-form-grid">
            <label>
              Type
              <select
                value={draft.type}
                onChange={(event) =>
                  setDraft({ ...draft, type: event.target.value as IssueType })
                }
              >
                {(["TASK", "BUG", "STORY"] as IssueType[]).map((item) => (
                  <option key={item}>{item}</option>
                ))}
              </select>
            </label>
            <label>
              Status
              <select
                value={draft.status}
                onChange={(event) =>
                  setDraft({
                    ...draft,
                    status: event.target.value as IssueStatus,
                  })
                }
              >
                {(editing
                  ? [
                      issues.find((issue) => issue.id === editing)?.status ??
                        draft.status,
                      ...allowedTransitions[
                        issues.find((issue) => issue.id === editing)?.status ??
                          draft.status
                      ],
                    ]
                  : statuses
                ).map((item) => (
                  <option key={item}>{item}</option>
                ))}
              </select>
            </label>
            <label>
              Priority
              <select
                value={draft.priority}
                onChange={(event) =>
                  setDraft({
                    ...draft,
                    priority: event.target.value as IssuePriority,
                  })
                }
              >
                {priorities.map((item) => (
                  <option key={item}>{item}</option>
                ))}
              </select>
            </label>
            <label>
              Assignee
              <select
                value={draft.assigneeId ?? ""}
                onChange={(event) =>
                  setDraft({ ...draft, assigneeId: event.target.value || null })
                }
              >
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
              <input
                type="date"
                value={draft.dueDate ?? ""}
                onChange={(event) =>
                  setDraft({ ...draft, dueDate: event.target.value || null })
                }
              />
            </label>
          </div>
          <div className="button-row">
            <button className="primary-button compact" disabled={busy}>
              {busy ? "Saving…" : "Save issue"}
            </button>
            <button
              type="button"
              className="text-action"
              onClick={() => {
                setCreating(false);
                setEditing("");
              }}
            >
              Cancel
            </button>
          </div>
        </form>
      )}
      {(loading || !active) && (
        <p className="subtle issue-loading" role="status">
          {active ? "Loading issues…" : "Loading workspace access…"}
        </p>
      )}
      <div className="issue-list">
        {active &&
          !loading &&
          issues.map((issue) => (
            <article
              className={`issue-row${issue.archivedAt ? " issue-archived" : ""}`}
              key={issue.id}
            >
              <div className="issue-main">
                <div>
                  <span
                    className={`status-pill issue-status-${issue.status.toLowerCase()}`}
                  >
                    {issue.status.replace("_", " ").toLowerCase()}
                  </span>
                  <span
                    className={`priority-label priority-${issue.priority.toLowerCase()}`}
                  >
                    {issue.priority.toLowerCase()}
                  </span>
                </div>
                <Link
                  className="issue-title-link"
                  to={`/workspaces/${workspaceId}/issues/${issue.issueKey}`}
                >
                  {issue.issueKey} · {issue.title}
                </Link>
                <small>
                  {issue.type.toLowerCase()} ·{" "}
                  {issue.assigneeName
                    ? `Assigned to ${issue.assigneeName}`
                    : "Unassigned"}
                  {issue.dueDate ? ` · Due ${issue.dueDate}` : ""}
                </small>
              </div>
              <div className="issue-actions">
                {!issue.archivedAt && projectActive && (
                  <button
                    className="text-action"
                    onClick={() => startEdit(issue)}
                  >
                    Edit
                  </button>
                )}
                {canManage &&
                  (issue.archivedAt ? (
                    <button
                      className="text-action"
                      disabled={busy || !projectActive}
                      onClick={() => void restore(issue)}
                    >
                      Restore
                    </button>
                  ) : (
                    projectActive && (
                      <button
                        className="remove-member-button"
                        disabled={busy}
                        onClick={() => void archive(issue)}
                      >
                        Archive
                      </button>
                    )
                  ))}
              </div>
              <details
                className="issue-detail"
                onToggle={(event) => {
                  const opened = event.currentTarget.open;
                  setExpandedIssueIds((current) => {
                    const next = new Set(current);
                    if (opened) next.add(issue.id);
                    else next.delete(issue.id);
                    return next;
                  });
                }}
              >
                <summary>Issue details, conversation &amp; history</summary>
                {expandedIssueIds.has(issue.id) && (
                  <>
                    <IssueOverview issue={issue} />
                    <IssueCollaboration
                      issueId={issue.id}
                      writable={!issue.archivedAt && projectActive}
                    />
                  </>
                )}
              </details>
            </article>
          ))}
        {active && !loading && !issues.length && (
          <p className="empty-copy">
            No matching issues. Create one to start tracking work.
          </p>
        )}
      </div>
      <div className="issue-pagination">
        <button
          className="secondary-button"
          disabled={page === 0 || loading || !active}
          onClick={() => setPage((current) => current - 1)}
        >
          Previous
        </button>
        <span>
          Page {page + 1} of {Math.max(1, Math.ceil(total / 10))} · {total}{" "}
          total
        </span>
        <button
          className="secondary-button"
          disabled={(page + 1) * 10 >= total || loading || !active}
          onClick={() => setPage((current) => current + 1)}
        >
          Next
        </button>
      </div>
    </section>
  );
}

function IssueOverview({ issue }: { issue: Issue }) {
  return (
    <div className="issue-overview">
      <div className="issue-overview-title">
        <span>{issue.issueKey}</span>
        <strong>{issue.title}</strong>
      </div>
      <p>{issue.description || "No description provided."}</p>
      <dl>
        <div>
          <dt>Reported by</dt>
          <dd>{issue.reporterName}</dd>
        </div>
        <div>
          <dt>Assignee</dt>
          <dd>{issue.assigneeName || "Unassigned"}</dd>
        </div>
        <div>
          <dt>Type</dt>
          <dd>{issue.type.toLowerCase()}</dd>
        </div>
        <div>
          <dt>Priority</dt>
          <dd>{issue.priority.toLowerCase()}</dd>
        </div>
        <div>
          <dt>Due date</dt>
          <dd>{issue.dueDate || "No due date"}</dd>
        </div>
        <div>
          <dt>Created</dt>
          <dd>{formatTimestamp(issue.createdAt)}</dd>
        </div>
        <div>
          <dt>Last updated</dt>
          <dd>{formatTimestamp(issue.updatedAt)}</dd>
        </div>
      </dl>
    </div>
  );
}

function formatTimestamp(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
