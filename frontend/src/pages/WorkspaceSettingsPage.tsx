import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, deleteWorkspace, updateWorkspace } from "../auth/api";
import { PageHeader } from "../components/ui/PageHeader";
import { useAppLayout } from "../layouts/app-layout-context";
import { useConfirm } from "../components/ui/ConfirmProvider";

export function WorkspaceSettingsPage() {
  const { confirm } = useConfirm();
  const { activeWorkspace, refreshWorkspaces } = useAppLayout();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const canManage =
    activeWorkspace?.role === "OWNER" || activeWorkspace?.role === "ADMIN";
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!activeWorkspace) return;
    const data = new FormData(event.currentTarget);
    setBusy(true);
    setError("");
    try {
      await updateWorkspace(
        activeWorkspace.id,
        String(data.get("name")).trim(),
        String(data.get("description") ?? "").trim(),
      );
      await refreshWorkspaces();
      setNotice("Workspace settings saved.");
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not update workspace settings.",
      );
    } finally {
      setBusy(false);
    }
  }
  async function remove() {
    if (
      !activeWorkspace ||
      !(await confirm({
        title: "Delete workspace?",
        message: `Delete “${activeWorkspace.name}” and its workspace data? This cannot be undone.`,
        confirmLabel: "Delete workspace",
        destructive: true,
      }))
    )
      return;
    setBusy(true);
    setError("");
    try {
      await deleteWorkspace(activeWorkspace.id);
      await refreshWorkspaces();
      navigate("/", { replace: true });
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not delete this workspace.",
      );
    } finally {
      setBusy(false);
    }
  }
  if (!activeWorkspace) return null;
  return (
    <>
      <PageHeader
        eyebrow="ADMINISTRATION"
        title="Workspace settings"
        description="Manage your workspace information and access."
      />
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
      {canManage ? (
        <form
          className="workspace-form settings-form"
          onSubmit={(event) => void save(event)}
        >
          <h2>Workspace information</h2>
          <label>
            Name
            <input
              name="name"
              required
              maxLength={100}
              defaultValue={activeWorkspace.name}
            />
          </label>
          <label>
            Description
            <textarea
              name="description"
              rows={4}
              maxLength={500}
              defaultValue={activeWorkspace.description ?? ""}
            />
          </label>
          <button className="primary-button compact" disabled={busy}>
            {busy ? "Saving…" : "Save changes"}
          </button>
        </form>
      ) : (
        <section className="detail-card">
          <h2>Workspace information</h2>
          <p>You have read-only access to workspace settings.</p>
          <dl className="detail-grid">
            <div>
              <dt>Name</dt>
              <dd>{activeWorkspace.name}</dd>
            </div>
            <div>
              <dt>Description</dt>
              <dd>{activeWorkspace.description || "No description"}</dd>
            </div>
          </dl>
        </section>
      )}
      <section className="settings-links">
        <h2>Workspace access</h2>
        <p>Review membership and roles for this workspace.</p>
        <Link
          className="secondary-button"
          to={`/workspaces/${activeWorkspace.id}/members`}
        >
          Manage members
        </Link>
      </section>
      {activeWorkspace.role === "OWNER" && (
        <section className="danger-zone">
          <h2>Delete workspace</h2>
          <p>Delete this workspace and its projects, issues, and membership.</p>
          <button
            className="remove-member-button"
            disabled={busy}
            onClick={() => void remove()}
          >
            Delete workspace
          </button>
        </section>
      )}
    </>
  );
}
