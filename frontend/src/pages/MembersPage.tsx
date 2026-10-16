import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import {
  ApiError,
  addWorkspaceMember,
  listWorkspaceMembers,
  removeWorkspaceMember,
  updateWorkspaceMemberRole,
  type MembershipRole,
  type WorkspaceMember,
} from "../auth/api";
import { PageHeader } from "../components/ui/PageHeader";
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from "../components/ui/StateMessage";
import { useAuth } from "../auth/useAuth";
import { useAppLayout } from "../layouts/app-layout-context";
import { useConfirm } from "../components/ui/ConfirmProvider";

export function MembersPage() {
  const { confirm } = useConfirm();
  const { activeWorkspace, refreshWorkspaces } = useAppLayout();
  const { user } = useAuth();
  const navigate = useNavigate();
  const workspaceId = activeWorkspace?.id ?? "";
  const isOwner = activeWorkspace?.role === "OWNER";
  const canManage =
    activeWorkspace?.role === "OWNER" || activeWorkspace?.role === "ADMIN";
  const [members, setMembers] = useState<WorkspaceMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setMembers(await listWorkspaceMembers(workspaceId));
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not load workspace members.",
      );
    } finally {
      setLoading(false);
    }
  }, [workspaceId]);
  useEffect(() => {
    void Promise.resolve().then(load);
  }, [load]);
  async function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    setBusy(true);
    setError("");
    try {
      await addWorkspaceMember(
        workspaceId,
        String(data.get("email")).trim(),
        String(data.get("role")) as "ADMIN" | "MEMBER",
      );
      form.reset();
      setNotice("Member added.");
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not add this member.",
      );
    } finally {
      setBusy(false);
    }
  }
  async function changeRole(
    member: WorkspaceMember,
    role: Exclude<MembershipRole, "OWNER">,
  ) {
    setBusy(true);
    setError("");
    try {
      await updateWorkspaceMemberRole(workspaceId, member.userId, role);
      setNotice("Member role updated.");
      await load();
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not update this role.",
      );
    } finally {
      setBusy(false);
    }
  }
  async function remove(member: WorkspaceMember) {
    const self = member.userId === user?.id;
    const action = self
      ? "leave this workspace"
      : `remove ${member.displayName}`;
    if (
      !(await confirm({
        title: self ? "Leave workspace?" : "Remove member?",
        message: `Are you sure you want to ${action}?`,
        confirmLabel: self ? "Leave workspace" : "Remove member",
        destructive: true,
      }))
    )
      return;
    setBusy(true);
    setError("");
    try {
      await removeWorkspaceMember(workspaceId, member.userId);
      if (self) {
        await refreshWorkspaces();
        navigate("/", { replace: true });
      } else {
        setNotice("Member removed.");
        await load();
      }
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not remove this member.",
      );
    } finally {
      setBusy(false);
    }
  }
  if (loading) return <LoadingState>Loading members…</LoadingState>;
  return (
    <>
      <PageHeader
        eyebrow="WORKSPACE"
        title="Members"
        description="People who can access this workspace."
      />
      {error && <ErrorState message={error} onRetry={() => void load()} />}
      {notice && (
        <p className="notice" role="status">
          {notice}
        </p>
      )}
      {canManage && (
        <form
          className="workspace-form member-invite-form"
          onSubmit={(event) => void add(event)}
        >
          <h2>Add a member</h2>
          <label>
            Email
            <input name="email" type="email" required />
          </label>
          <label>
            Role
            <select name="role">
              <option value="MEMBER">Member</option>
              {isOwner && <option value="ADMIN">Admin</option>}
            </select>
          </label>
          <button className="primary-button compact" disabled={busy}>
            {busy ? "Adding…" : "Add member"}
          </button>
          <p className="member-note">
            The person must already have a Chila account. Invitations are not
            available yet.
          </p>
        </form>
      )}
      {members.length ? (
        <section className="member-directory">
          <div className="member-directory-head">
            <span>Member</span>
            <span>Role</span>
            <span>Joined</span>
            <span>Actions</span>
          </div>
          {members.map((member) => {
            const mayManage =
              canManage &&
              member.role !== "OWNER" &&
              (isOwner || member.role !== "ADMIN");
            const self = member.userId === user?.id;
            return (
              <article className="member-directory-row" key={member.userId}>
                <div className="member-identity">
                  <span className="sidebar-avatar">
                    {member.displayName.slice(0, 1).toUpperCase()}
                  </span>
                  <span>
                    <strong>
                      {member.displayName}
                      {self ? " (you)" : ""}
                    </strong>
                    <small>{member.email}</small>
                  </span>
                </div>
                <span className="role-pill">{member.role.toLowerCase()}</span>
                <time>{new Date(member.joinedAt).toLocaleDateString()}</time>
                <div className="member-actions">
                  {mayManage && (
                    <select
                      aria-label={`Role for ${member.displayName}`}
                      value={member.role}
                      disabled={busy}
                      onChange={(event) =>
                        void changeRole(
                          member,
                          event.target.value as "ADMIN" | "MEMBER",
                        )
                      }
                    >
                      <option value="MEMBER">Member</option>
                      {isOwner && <option value="ADMIN">Admin</option>}
                    </select>
                  )}
                  {member.role !== "OWNER" && (mayManage || self) && (
                    <button
                      className="remove-member-button"
                      disabled={busy}
                      onClick={() => void remove(member)}
                    >
                      {self ? "Leave" : "Remove"}
                    </button>
                  )}
                </div>
              </article>
            );
          })}
        </section>
      ) : (
        <EmptyState
          title="No members found"
          detail="Workspace members will appear here."
        />
      )}
    </>
  );
}
