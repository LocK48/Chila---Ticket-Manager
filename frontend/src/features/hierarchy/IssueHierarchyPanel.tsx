import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import {
  addIssueLink,
  ApiError,
  assignIssueEpic,
  assignIssueParent,
  assignIssueRelease,
  getIssueReference,
  getIssueRelationships,
  listProjectEpics,
  listProjectReleases,
  removeIssueLink,
  type Epic,
  type Issue,
  type IssueLinkType,
  type IssueRelationships,
  type Project,
  type ProjectRelease,
} from "../../auth/api";

export function IssueHierarchyPanel({
  workspaceId,
  project,
  issue,
  writable,
  onIssue,
}: {
  workspaceId: string;
  project: Project;
  issue: Issue;
  writable: boolean;
  onIssue: (issue: Issue) => void;
}) {
  const [epics, setEpics] = useState<Epic[]>([]);
  const [releases, setReleases] = useState<ProjectRelease[]>([]);
  const [relationships, setRelationships] = useState<IssueRelationships>({
    parent: null,
    children: [],
    links: [],
  });
  const [parentKey, setParentKey] = useState("");
  const [targetKey, setTargetKey] = useState("");
  const [linkType, setLinkType] = useState<IssueLinkType>("RELATES_TO");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const load = useCallback(async () => {
    try {
      const [epicList, releaseList, relations] = await Promise.all([
        listProjectEpics(project.id),
        listProjectReleases(project.id),
        getIssueRelationships(issue.id),
      ]);
      setEpics(epicList);
      setReleases(releaseList);
      setRelationships(relations);
      setError("");
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not load issue relationships.",
      );
    }
  }, [issue.id, project.id]);
  useEffect(() => {
    void Promise.resolve().then(load);
  }, [load]);
  async function update(action: () => Promise<Issue>) {
    setBusy(true);
    setError("");
    try {
      onIssue(await action());
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not update issue relationships.",
      );
    } finally {
      setBusy(false);
    }
  }
  async function setParent(event: FormEvent) {
    event.preventDefault();
    if (!parentKey.trim()) return void setError("Enter the parent issue key.");
    setBusy(true);
    setError("");
    try {
      const parent = await getIssueReference(workspaceId, parentKey.trim());
      onIssue(await assignIssueParent(issue.id, parent.id));
      setParentKey("");
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not set the parent issue.",
      );
    } finally {
      setBusy(false);
    }
  }
  async function addLink(event: FormEvent) {
    event.preventDefault();
    if (!targetKey.trim()) return void setError("Enter an issue key to link.");
    setBusy(true);
    setError("");
    try {
      const target = await getIssueReference(workspaceId, targetKey.trim());
      await addIssueLink(issue.id, target.id, linkType);
      setTargetKey("");
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not add the issue link.",
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="detail-card hierarchy-issue-panel">
      <h2>Planning and relationships</h2>
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      <div className="hierarchy-fields">
        <label>
          Epic
          <select
            disabled={!writable || busy}
            value={issue.epicId ?? ""}
            onChange={(e) =>
              void update(() =>
                assignIssueEpic(issue.id, e.target.value || null),
              )
            }
          >
            <option value="">No epic</option>
            {epics.map((epic) => (
              <option key={epic.id} value={epic.id}>
                {epic.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Release
          <select
            disabled={!writable || busy}
            value={issue.releaseId ?? ""}
            onChange={(e) =>
              void update(() =>
                assignIssueRelease(issue.id, e.target.value || null),
              )
            }
          >
            <option value="">No release</option>
            {releases
              .filter(
                (release) =>
                  release.state !== "ARCHIVED" ||
                  release.id === issue.releaseId,
              )
              .map((release) => (
                <option key={release.id} value={release.id}>
                  {release.name} · {release.state.toLowerCase()}
                </option>
              ))}
          </select>
        </label>
      </div>
      <div className="hierarchy-relation-group">
        <h3>Parent and subtasks</h3>
        {relationships.parent ? (
          <p>
            Parent:{" "}
            <Link
              to={`/workspaces/${workspaceId}/issues/${relationships.parent.issueKey}`}
            >
              {relationships.parent.issueKey} · {relationships.parent.title}
            </Link>
            {writable && (
              <button
                className="text-action"
                disabled={busy}
                onClick={() =>
                  void update(() => assignIssueParent(issue.id, null))
                }
              >
                Remove parent
              </button>
            )}
          </p>
        ) : (
          <p className="empty-copy">No parent issue.</p>
        )}
        {relationships.children.map((child) => (
          <p className="relationship-chip" key={child.id}>
            <Link to={`/workspaces/${workspaceId}/issues/${child.issueKey}`}>
              {child.issueKey} · {child.title}
            </Link>
            <small>{child.status.replace("_", " ")}</small>
          </p>
        ))}
        {writable && (
          <form
            className="inline-link-form"
            onSubmit={(e) => void setParent(e)}
          >
            <input
              aria-label="Parent issue key"
              placeholder="Parent key, e.g. WEB-12"
              value={parentKey}
              onChange={(e) => setParentKey(e.target.value)}
            />
            <button className="secondary-button compact" disabled={busy}>
              Set parent
            </button>
          </form>
        )}
      </div>
      <div className="hierarchy-relation-group">
        <h3>Issue links</h3>
        {relationships.links.map((link) => {
          const outgoing = link.sourceIssueId === issue.id;
          const otherKey = outgoing ? link.targetIssueKey : link.sourceIssueKey;
          const otherTitle = outgoing ? link.targetTitle : link.sourceTitle;
          const verb =
            link.type === "RELATES_TO"
              ? "relates to"
              : outgoing
                ? link.type === "BLOCKS"
                  ? "blocks"
                  : "depends on"
                : link.type === "BLOCKS"
                  ? "is blocked by"
                  : "is required by";
          return (
            <p className="relationship-chip" key={link.id}>
              <span>{verb}</span>
              <Link to={`/workspaces/${workspaceId}/issues/${otherKey}`}>
                {otherKey} · {otherTitle}
              </Link>
              {writable && (
                <button
                  className="text-action"
                  disabled={busy}
                  onClick={() => {
                    setBusy(true);
                    void removeIssueLink(issue.id, link.id)
                      .then(load)
                      .catch((e) =>
                        setError(
                          e instanceof ApiError
                            ? e.message
                            : "Could not remove issue link.",
                        ),
                      )
                      .finally(() => setBusy(false));
                  }}
                >
                  Remove
                </button>
              )}
            </p>
          );
        })}
        {writable && (
          <form className="inline-link-form" onSubmit={(e) => void addLink(e)}>
            <input
              aria-label="Linked issue key"
              placeholder="Issue key to link"
              value={targetKey}
              onChange={(e) => setTargetKey(e.target.value)}
            />
            <select
              aria-label="Issue link type"
              value={linkType}
              onChange={(e) => setLinkType(e.target.value as IssueLinkType)}
            >
              <option value="RELATES_TO">relates to</option>
              <option value="BLOCKS">blocks</option>
              <option value="DEPENDS_ON">depends on</option>
            </select>
            <button className="secondary-button compact" disabled={busy}>
              Add link
            </button>
          </form>
        )}
      </div>
    </section>
  );
}
