import { Link, Navigate } from "react-router-dom";
import { useAppLayout } from "../layouts/app-layout-context";
import { EmptyState } from "../components/ui/StateMessage";

export function WorkspaceRedirect() {
  const { workspaces } = useAppLayout();
  const first = workspaces[0];
  if (first)
    return (
      <Navigate
        to={`/workspaces/${encodeURIComponent(first.id)}/overview`}
        replace
      />
    );
  return (
    <section className="welcome-empty">
      <p className="eyebrow">WELCOME TO CHILA</p>
      <h1>Your team’s work, in one place.</h1>
      <EmptyState
        title="Create your first workspace"
        detail="A workspace keeps your people, projects, and issues together."
      />
      <p className="subtle">
        Use “Create workspace” in the sidebar to get started.
      </p>
      <Link className="secondary-button welcome-profile-link" to="/profile">
        View your profile
      </Link>
    </section>
  );
}
