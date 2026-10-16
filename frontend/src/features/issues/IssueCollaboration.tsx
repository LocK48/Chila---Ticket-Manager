import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  addIssueComment,
  ApiError,
  listIssueActivities,
  listIssueComments,
  type IssueActivity,
  type IssueComment,
} from "../../auth/api";

export function IssueCollaboration({
  issueId,
  writable,
}: {
  issueId: string;
  writable: boolean;
}) {
  const [comments, setComments] = useState<IssueComment[]>([]);
  const [activities, setActivities] = useState<IssueActivity[]>([]);
  const [body, setBody] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [commentPage, activityPage] = await Promise.all([
        listIssueComments(issueId),
        listIssueActivities(issueId),
      ]);
      setComments(commentPage.items);
      setActivities(activityPage.items);
      setError("");
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not load this issue’s conversation.",
      );
    } finally {
      setLoading(false);
    }
  }, [issueId]);
  useEffect(() => {
    void Promise.resolve().then(load);
  }, [load]);
  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await addIssueComment(issueId, body.trim());
      setBody("");
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not add your comment.",
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="issue-collaboration">
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      <h6>Comments</h6>
      {writable && (
        <form className="comment-form" onSubmit={(event) => void submit(event)}>
          <label className="visually-hidden" htmlFor={`comment-${issueId}`}>
            Add a comment
          </label>
          <textarea
            id={`comment-${issueId}`}
            required
            maxLength={5000}
            rows={2}
            placeholder="Share an update with your team…"
            value={body}
            onChange={(event) => setBody(event.target.value)}
          />
          <button
            className="primary-button compact"
            disabled={busy || !body.trim()}
          >
            {busy ? "Posting…" : "Comment"}
          </button>
        </form>
      )}
      {loading && (
        <p className="subtle" role="status">
          Loading conversation…
        </p>
      )}
      {!loading && (
        <div className="comment-list">
          {comments.map((comment) => (
            <article className="comment-item" key={comment.id}>
              <div>
                <strong>{comment.authorName}</strong>
                <time>{formatDate(comment.createdAt)}</time>
              </div>
              <p>{comment.body}</p>
            </article>
          ))}
          {!comments.length && <p className="empty-copy">No comments yet.</p>}
        </div>
      )}
      <h6 className="activity-title">Activity</h6>
      {!loading && (
        <ol className="activity-list">
          {activities.map((item) => (
            <li key={item.id}>
              <span className="activity-dot" />
              <div>
                <strong>{item.actorName}</strong> {activityText(item)}
                <time>{formatDate(item.createdAt)}</time>
              </div>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

function activityText(item: IssueActivity): string {
  if (item.action === "CREATED") return "created this issue.";
  if (item.action === "ARCHIVED") return "archived this issue.";
  if (item.action === "RESTORED") return "restored this issue.";
  if (item.action === "COMMENTED") return "left a comment.";
  if (item.action === "UPDATED" && item.fieldName === "description")
    return "updated the description.";
  if (item.action === "UPDATED")
    return `changed ${item.fieldName ?? "a field"}${item.oldValue ? ` from “${item.oldValue}”` : ""}${item.newValue ? ` to “${item.newValue}”` : ""}.`;
  return "updated this issue.";
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
