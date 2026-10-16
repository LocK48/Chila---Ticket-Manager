import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import {
  ApiError,
  createEpic,
  createRelease,
  listDueIssues,
  listEpicIssues,
  listProjectEpics,
  listProjectReleases,
  updateEpic,
  updateRelease,
  type Epic,
  type EpicInput,
  type IssuePage,
  type IssueStatus,
  type Project,
  type ProjectRelease,
  type ReleaseInput,
  type ReleaseState,
} from "../../auth/api";

export function EpicsPage({
  project,
  canManage,
}: {
  project: Project;
  canManage: boolean;
}) {
  const [epics, setEpics] = useState<Epic[]>([]);
  const [selected, setSelected] = useState<Epic | null>(null);
  const [issues, setIssues] = useState<Awaited<
    ReturnType<typeof listEpicIssues>
  > | null>(null);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<IssueStatus | "">("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [editingId, setEditingId] = useState("");
  const [draft, setDraft] = useState<EpicInput>({
    name: "",
    summary: "",
    color: "#6554C0",
  });
  const load = useCallback(async () => {
    try {
      setEpics(await listProjectEpics(project.id));
      setError("");
    } catch (e) {
      setError(message(e, "Could not load epics."));
    }
  }, [project.id]);
  useEffect(() => {
    void Promise.resolve().then(load);
  }, [load]);
  useEffect(() => {
    if (!selected) {
      return;
    }
    void Promise.resolve()
      .then(() =>
        listEpicIssues(project.id, selected.id, status || undefined, search),
      )
      .then(setIssues)
      .catch((e) => setError(message(e, "Could not load epic issues.")));
  }, [project.id, search, selected, status]);
  async function save(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    try {
      if (editingId) await updateEpic(project.id, editingId, draft);
      else await createEpic(project.id, draft);
      setDraft({ name: "", summary: "", color: "#6554C0" });
      setEditingId("");
      setNotice("Epic saved.");
      await load();
    } catch (e) {
      setError(message(e, "Could not save the epic."));
    } finally {
      setBusy(false);
    }
  }
  function edit(epic: Epic) {
    setEditingId(epic.id);
    setDraft({ name: epic.name, summary: epic.summary, color: epic.color });
  }
  return (
    <div className="hierarchy-page">
      <header className="section-heading">
        <div>
          <h2>Epics</h2>
          <p>Group related issues and track delivery progress.</p>
        </div>
      </header>
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
      {canManage && (
        <form
          className="workspace-form hierarchy-form"
          onSubmit={(e) => void save(e)}
        >
          <h3>{editingId ? "Edit epic" : "Create epic"}</h3>
          <label>
            Name
            <input
              required
              maxLength={120}
              value={draft.name}
              onChange={(e) => setDraft({ ...draft, name: e.target.value })}
            />
          </label>
          <label>
            Summary
            <textarea
              rows={2}
              maxLength={2000}
              value={draft.summary ?? ""}
              onChange={(e) =>
                setDraft({ ...draft, summary: e.target.value || null })
              }
            />
          </label>
          <label>
            Color
            <input
              type="color"
              value={draft.color}
              onChange={(e) => setDraft({ ...draft, color: e.target.value })}
            />
          </label>
          <div className="button-row">
            <button className="primary-button compact" disabled={busy}>
              {busy ? "Saving…" : editingId ? "Save epic" : "Create epic"}
            </button>
            {editingId && (
              <button
                type="button"
                className="text-action"
                onClick={() => {
                  setEditingId("");
                  setDraft({ name: "", summary: "", color: "#6554C0" });
                }}
              >
                Cancel
              </button>
            )}
          </div>
        </form>
      )}
      <div className="epic-grid">
        {epics.map((epic) => (
          <article
            className={`epic-card ${selected?.id === epic.id ? "selected" : ""}`}
            key={epic.id}
            style={{ borderTopColor: epic.color }}
          >
            <button
              className="epic-select"
              onClick={() =>
                setSelected(selected?.id === epic.id ? null : epic)
              }
            >
              <strong>{epic.name}</strong>
              <span>{epic.summary || "No summary"}</span>
              <span>
                {epic.completedIssues} of {epic.totalIssues} issues complete
              </span>
              <span className="progress-track">
                <i
                  style={{
                    width: `${epic.totalIssues ? (epic.completedIssues / epic.totalIssues) * 100 : 0}%`,
                    background: epic.color,
                  }}
                />
              </span>
            </button>
            {canManage && (
              <button className="text-action" onClick={() => edit(epic)}>
                Edit
              </button>
            )}
          </article>
        ))}
      </div>
      {!epics.length && <p className="empty-copy">No epics yet.</p>}
      {selected && (
        <section className="detail-card epic-issue-list">
          <div className="section-heading">
            <div>
              <h3>{selected.name}</h3>
              <p>{selected.summary || "Issues in this epic"}</p>
            </div>
            <button className="text-action" onClick={() => setSelected(null)}>
              Close
            </button>
          </div>
          <div className="filter-row">
            <input
              aria-label="Search epic issues"
              placeholder="Search by key or title"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <select
              aria-label="Filter epic by status"
              value={status}
              onChange={(e) => setStatus(e.target.value as IssueStatus | "")}
            >
              <option value="">All statuses</option>
              {["TODO", "IN_PROGRESS", "IN_REVIEW", "DONE"].map((v) => (
                <option key={v} value={v}>
                  {v.replace("_", " ")}
                </option>
              ))}
            </select>
          </div>
          {issues?.items.map((issue) => (
            <Link
              className="hierarchy-issue-row"
              key={issue.id}
              to={`/workspaces/${project.workspaceId}/issues/${issue.issueKey}`}
            >
              <strong>{issue.issueKey}</strong>
              <span>{issue.title}</span>
              <small>{issue.status.replace("_", " ")}</small>
            </Link>
          ))}
          {issues && !issues.items.length && (
            <p className="empty-copy">No matching issues in this epic.</p>
          )}
          {issues && issues.totalPages > 1 && (
            <p className="member-note">
              Showing the first 100 matching issues. Narrow the search to find
              more.
            </p>
          )}
        </section>
      )}
    </div>
  );
}

export function ReleasesPage({
  project,
  canManage,
}: {
  project: Project;
  canManage: boolean;
}) {
  const [releases, setReleases] = useState<ProjectRelease[]>([]);
  const [epics, setEpics] = useState<Epic[]>([]);
  const [dueIssues, setDueIssues] = useState<IssuePage | null>(null);
  const [draft, setDraft] = useState<ReleaseInput>({
    name: "",
    description: "",
    startDate: "",
    releaseDate: "",
    state: null,
  });
  const [editingId, setEditingId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const load = useCallback(async () => {
    try {
      const [releaseList, epicList, duePage] = await Promise.all([
        listProjectReleases(project.id),
        listProjectEpics(project.id),
        listDueIssues(project.id),
      ]);
      setReleases(releaseList);
      setEpics(epicList);
      setDueIssues(duePage);
      setError("");
    } catch (e) {
      setError(message(e, "Could not load the release timeline."));
    }
  }, [project.id]);
  useEffect(() => {
    void Promise.resolve().then(load);
  }, [load]);
  async function save(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    try {
      if (editingId) await updateRelease(project.id, editingId, draft);
      else await createRelease(project.id, draft);
      setDraft({
        name: "",
        description: "",
        startDate: "",
        releaseDate: "",
        state: null,
      });
      setEditingId("");
      setNotice("Release saved.");
      await load();
    } catch (e) {
      setError(message(e, "Could not save release."));
    } finally {
      setBusy(false);
    }
  }
  function edit(item: ProjectRelease) {
    setEditingId(item.id);
    setDraft({
      name: item.name,
      description: item.description,
      startDate: item.startDate,
      releaseDate: item.releaseDate,
      state: item.state,
    });
  }
  return (
    <div className="hierarchy-page">
      <header className="section-heading">
        <div>
          <h2>Releases and timeline</h2>
          <p>
            Plan versions against project dates. This is a lightweight release
            timeline, not a Gantt chart.
          </p>
        </div>
      </header>
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
      {canManage && (
        <form
          className="workspace-form hierarchy-form release-form"
          onSubmit={(e) => void save(e)}
        >
          <h3>{editingId ? "Edit release" : "Create release"}</h3>
          <label>
            Name
            <input
              required
              maxLength={120}
              value={draft.name}
              onChange={(e) => setDraft({ ...draft, name: e.target.value })}
            />
          </label>
          <label>
            Description
            <textarea
              rows={2}
              maxLength={2000}
              value={draft.description ?? ""}
              onChange={(e) =>
                setDraft({ ...draft, description: e.target.value || null })
              }
            />
          </label>
          <label>
            Start date
            <input
              type="date"
              value={draft.startDate ?? ""}
              onChange={(e) =>
                setDraft({ ...draft, startDate: e.target.value || null })
              }
            />
          </label>
          <label>
            Release date
            <input
              type="date"
              value={draft.releaseDate ?? ""}
              onChange={(e) =>
                setDraft({ ...draft, releaseDate: e.target.value || null })
              }
            />
          </label>
          <label>
            State
            <select
              value={draft.state ?? "UNRELEASED"}
              onChange={(e) =>
                setDraft({ ...draft, state: e.target.value as ReleaseState })
              }
            >
              {["UNRELEASED", "RELEASED", "ARCHIVED"].map((s) => (
                <option key={s}>{s}</option>
              ))}
            </select>
          </label>
          <div className="button-row">
            <button className="primary-button compact" disabled={busy}>
              {busy ? "Saving…" : editingId ? "Save release" : "Create release"}
            </button>
            {editingId && (
              <button
                type="button"
                className="text-action"
                onClick={() => {
                  setEditingId("");
                  setDraft({
                    name: "",
                    description: "",
                    startDate: "",
                    releaseDate: "",
                    state: null,
                  });
                }}
              >
                Cancel
              </button>
            )}
          </div>
        </form>
      )}
      <section className="detail-card">
        <h3>Epic workstreams</h3>
        <p className="member-note">
          Epics show live completion progress. Epic date scheduling can be added
          when the roadmap requires it.
        </p>
        {epics.map((epic) => (
          <Link
            className="epic-timeline-row"
            key={epic.id}
            to={`/workspaces/${project.workspaceId}/projects/${project.id}/epics`}
          >
            <span
              className="epic-color-dot"
              style={{ background: epic.color }}
            />
            <strong>{epic.name}</strong>
            <span>
              {epic.completedIssues}/{epic.totalIssues} complete
            </span>
          </Link>
        ))}
        {!epics.length && <p className="empty-copy">No epics to show.</p>}
      </section>
      <section className="detail-card">
        <h3>Issue due dates</h3>
        {dueIssues?.items.map((issue) => (
          <Link
            className="hierarchy-issue-row"
            key={issue.id}
            to={`/workspaces/${project.workspaceId}/issues/${issue.issueKey}`}
          >
            <strong>{issue.issueKey}</strong>
            <span>{issue.title}</span>
            <small>{issue.dueDate}</small>
          </Link>
        ))}
        {dueIssues && !dueIssues.items.length && (
          <p className="empty-copy">No issue due dates are scheduled.</p>
        )}
        {Boolean(dueIssues?.totalPages && dueIssues.totalPages > 1) && (
          <p className="member-note">Showing the first 100 due dates.</p>
        )}
      </section>
      <div className="release-timeline">
        {releases.map((item) => (
          <article className="release-item" key={item.id}>
            <span className="timeline-marker" />
            <div className="release-content">
              <div className="section-heading">
                <div>
                  <h3>{item.name}</h3>
                  <p>{item.description || "No release notes yet."}</p>
                </div>
                <span
                  className={`release-state state-${item.state.toLowerCase()}`}
                >
                  {item.state.replace("_", " ")}
                </span>
              </div>
              <dl className="detail-grid">
                <div>
                  <dt>Start</dt>
                  <dd>{item.startDate || "Not scheduled"}</dd>
                </div>
                <div>
                  <dt>Release</dt>
                  <dd>{item.releaseDate || "Not scheduled"}</dd>
                </div>
                <div>
                  <dt>Issues</dt>
                  <dd>{item.issueCount}</dd>
                </div>
              </dl>
              {canManage && (
                <button className="text-action" onClick={() => edit(item)}>
                  Edit release
                </button>
              )}
            </div>
          </article>
        ))}
      </div>
      {!releases.length && <p className="empty-copy">No releases planned.</p>}
    </div>
  );
}

function message(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback;
}
