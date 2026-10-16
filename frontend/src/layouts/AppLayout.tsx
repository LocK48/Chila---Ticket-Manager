import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link, NavLink, Outlet, useMatch, useNavigate } from "react-router-dom";
import { ApiError } from "../api/client";
import { createWorkspace, listWorkspaces } from "../api/workspaces";
import { type Workspace } from "../api/types";
import { useAuth } from "../auth/useAuth";
import { NotificationsPanel } from "../features/notifications/NotificationsPanel";
import type { AppLayoutContext } from "./app-layout-context";

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const workspaceMatch = useMatch("/workspaces/:workspaceId/*");
  const workspaceId = workspaceMatch?.params.workspaceId;
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [mobileOpen, setMobileOpen] = useState(false);

  const refreshWorkspaces = useCallback(async () => {
    const items = await listWorkspaces();
    setWorkspaces(items);
    return items;
  }, []);

  useEffect(() => {
    let live = true;
    listWorkspaces()
      .then((items) => {
        if (live) setWorkspaces(items);
      })
      .catch((cause: unknown) => {
        if (live)
          setError(messageFor(cause, "Could not load your workspaces."));
      })
      .finally(() => {
        if (live) setLoading(false);
      });
    return () => {
      live = false;
    };
  }, []);

  const activeWorkspace =
    workspaces.find((workspace) => workspace.id === workspaceId) ?? null;
  const context: AppLayoutContext = {
    workspaces,
    activeWorkspace,
    refreshWorkspaces,
  };

  async function switchWorkspace(nextId: string) {
    if (nextId) navigate(`/workspaces/${encodeURIComponent(nextId)}/overview`);
  }

  async function createNewWorkspace(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const name = String(data.get("name") ?? "").trim();
    const description = String(data.get("description") ?? "").trim();
    setError("");
    try {
      const workspace = await createWorkspace(name, description);
      await refreshWorkspaces();
      setMobileOpen(false);
      navigate(`/workspaces/${encodeURIComponent(workspace.id)}/overview`);
    } catch (cause) {
      setError(messageFor(cause, "Could not create the workspace."));
    }
  }

  async function signOut() {
    try {
      await logout();
      navigate("/login", { replace: true });
    } catch {
      navigate("/login", {
        replace: true,
        state: { notice: "Could not reach the server to clear your session." },
      });
    }
  }

  if (loading)
    return <main className="loading-screen">Loading your workspaces…</main>;
  if (error && !workspaces.length)
    return (
      <main className="loading-screen">
        <div className="app-load-error" role="alert">
          <p>{error}</p>
          <button
            className="secondary-button"
            onClick={() => window.location.reload()}
          >
            Try again
          </button>
        </div>
      </main>
    );

  const noWorkspace = workspaces.length === 0;
  const links = activeWorkspace
    ? [
        {
          to: `/workspaces/${activeWorkspace.id}/overview`,
          label: "Overview",
          icon: "◫",
          end: true,
        },
        {
          to: `/workspaces/${activeWorkspace.id}/projects`,
          label: "Projects",
          icon: "▦",
          end: false,
        },
        {
          to: `/workspaces/${activeWorkspace.id}/issues`,
          label: "All issues",
          icon: "☷",
          end: false,
        },
        {
          to: `/workspaces/${activeWorkspace.id}/my-issues`,
          label: "My issues",
          icon: "✓",
          end: true,
        },
        {
          to: `/workspaces/${activeWorkspace.id}/members`,
          label: "Members",
          icon: "♙",
          end: true,
        },
      ]
    : [];
  return (
    <div className={`app-frame${mobileOpen ? " mobile-nav-open" : ""}`}>
      <>
        <button
          className="mobile-nav-scrim"
          aria-label="Close navigation"
          onClick={() => setMobileOpen(false)}
        />
        <aside className="app-sidebar" aria-label="Main navigation">
          <Link
            className="app-brand"
            to={
              activeWorkspace
                ? `/workspaces/${activeWorkspace.id}/overview`
                : "/"
            }
            onClick={() => setMobileOpen(false)}
          >
            <span className="brand-glyph">c</span>
            <span>chila</span>
          </Link>
          <div className="sidebar-workspace">
            <label htmlFor="workspace-switcher">Workspace</label>
            <select
              id="workspace-switcher"
              value={activeWorkspace?.id ?? ""}
              disabled={noWorkspace}
              onChange={(event) => void switchWorkspace(event.target.value)}
            >
              {workspaces.map((workspace) => (
                <option key={workspace.id} value={workspace.id}>
                  {workspace.name}
                </option>
              ))}
            </select>
            <details className="workspace-create-disclosure">
              <summary>+ Create workspace</summary>
              <form onSubmit={(event) => void createNewWorkspace(event)}>
                <label>
                  Name
                  <input required name="name" maxLength={100} />
                </label>
                <label>
                  Description <span className="field-hint">Optional</span>
                  <textarea name="description" rows={2} maxLength={500} />
                </label>
                <button className="primary-button compact">
                  Create workspace
                </button>
              </form>
            </details>
          </div>
          {activeWorkspace && (
            <>
              <nav className="sidebar-nav" aria-label="Workspace">
                <p className="sidebar-section-label">WORKSPACE</p>
                {links.map((link) => (
                  <NavLink
                    key={link.to}
                    to={link.to}
                    end={link.end}
                    onClick={() => setMobileOpen(false)}
                    className={({ isActive }) =>
                      `sidebar-link${isActive ? " is-active" : ""}`
                    }
                  >
                    <span className="sidebar-icon" aria-hidden="true">
                      {link.icon}
                    </span>
                    {link.label}
                  </NavLink>
                ))}
                {(activeWorkspace.role === "OWNER" ||
                  activeWorkspace.role === "ADMIN") && (
                  <>
                    <p className="sidebar-section-label sidebar-settings-label">
                      SETTINGS
                    </p>
                    <NavLink
                      to={`/workspaces/${activeWorkspace.id}/settings`}
                      onClick={() => setMobileOpen(false)}
                      className={({ isActive }) =>
                        `sidebar-link${isActive ? " is-active" : ""}`
                      }
                    >
                      <span className="sidebar-icon" aria-hidden="true">
                        ⚙
                      </span>
                      Workspace settings
                    </NavLink>
                  </>
                )}
              </nav>
            </>
          )}
          <div className="sidebar-user">
            <span className="sidebar-avatar" aria-hidden="true">
              {user?.displayName.slice(0, 1).toUpperCase()}
            </span>
            <div className="sidebar-user-copy">
              <strong>{user?.displayName}</strong>
              <small>{user?.email}</small>
            </div>
            <details className="user-menu">
              <summary aria-label="Account menu">•••</summary>
              <div className="user-menu-popover">
                <Link
                  to={
                    activeWorkspace
                      ? `/workspaces/${activeWorkspace.id}/profile`
                      : "/profile"
                  }
                  onClick={() => setMobileOpen(false)}
                >
                  My profile
                </Link>
                <button onClick={() => void signOut()}>Sign out</button>
              </div>
            </details>
          </div>
        </aside>
      </>
      <div className="app-main-column">
        <header className="app-topbar">
          <button
            className="mobile-menu-button"
            aria-label="Open navigation"
            aria-expanded={mobileOpen}
            onClick={() => setMobileOpen(true)}
          >
            ☰
          </button>
          <span className="app-topbar-context">
            {activeWorkspace?.name ?? "Your workspace"}
          </span>
          <NotificationsPanel />
        </header>
        {error && (
          <div className="app-global-error" role="alert">
            {error}
            <button aria-label="Dismiss error" onClick={() => setError("")}>
              ×
            </button>
          </div>
        )}
        <main className="app-content">
          <Outlet context={context} />
        </main>
      </div>
    </div>
  );
}

function messageFor(cause: unknown, fallback: string) {
  return cause instanceof ApiError ? cause.message || fallback : fallback;
}
