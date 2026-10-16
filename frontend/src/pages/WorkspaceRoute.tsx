import { Link, Navigate, Outlet, useParams } from "react-router-dom";
import { useAppLayout } from "../layouts/app-layout-context";

export function WorkspaceRoute() {
  const { workspaceId } = useParams();
  const layoutContext = useAppLayout();
  const { workspaces, activeWorkspace } = layoutContext;
  if (!workspaces.length) return <Navigate to="/" replace />;
  if (!activeWorkspace || activeWorkspace.id !== workspaceId) {
    return (
      <section className="access-state">
        <p className="eyebrow">WORKSPACE UNAVAILABLE</p>
        <h1>You don’t have access to this workspace.</h1>
        <p className="subtle">Choose one of your workspaces to continue.</p>
        <Link className="primary-button compact" to="/">
          Go to your workspaces
        </Link>
      </section>
    );
  }
  return <Outlet context={layoutContext} />;
}
