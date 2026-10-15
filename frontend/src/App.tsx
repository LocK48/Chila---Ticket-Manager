import type { ReactNode } from 'react'
import { BrowserRouter, Link, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { useAuth } from './auth/useAuth'
import { AppLayout } from './layouts/AppLayout'
import { IssueDetailsPage } from './pages/IssueDetailsPage'
import { LoginPage, RegisterPage } from './pages/EntryPages'
import { MembersPage } from './pages/MembersPage'
import { OverviewPage } from './pages/OverviewPage'
import { ProfilePage } from './pages/ProfilePage'
import { ProjectBoardTab, ProjectDetailsPage, ProjectIssuesTab, ProjectOverviewTab, ProjectSettingsTab } from './pages/ProjectDetailsPage'
import { ProjectsPage } from './pages/ProjectsPage'
import { WorkspaceIssuesPage } from './pages/WorkspaceIssuesPage'
import { WorkspaceRedirect } from './pages/WorkspaceRedirect'
import { WorkspaceRoute } from './pages/WorkspaceRoute'
import { WorkspaceSettingsPage } from './pages/WorkspaceSettingsPage'
import './App.css'

function RequireAuth() {
  const { user, loading, error } = useAuth()
  const location = useLocation()
  if (loading) return <main className="loading-screen">Opening Chila…</main>
  if (error) return <main className="loading-screen form-error" role="alert">{error}</main>
  return user ? <Outlet /> : <Navigate to="/login" replace state={{ from: location }} />
}

function GuestOnly({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <main className="loading-screen">Opening Chila…</main>
  return user ? <Navigate to="/" replace /> : children
}

function NotFoundPage() {
  return <main className="not-found"><p className="eyebrow">404</p><h1>Page not found</h1><p className="subtle">That page may have moved or you may not have access to it.</p><Link className="primary-button compact" to="/">Go to Chila</Link></main>
}

function AppRoutes() {
  return <Routes>
    <Route path="/login" element={<GuestOnly><LoginPage /></GuestOnly>} />
    <Route path="/register" element={<GuestOnly><RegisterPage /></GuestOnly>} />
    <Route element={<RequireAuth />}>
      <Route element={<AppLayout />}>
        <Route index element={<WorkspaceRedirect />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="workspaces/:workspaceId" element={<WorkspaceRoute />}>
          <Route path="overview" element={<OverviewPage />} />
          <Route path="projects" element={<ProjectsPage />} />
          <Route path="projects/:projectId" element={<ProjectDetailsPage />}>
            <Route index element={<ProjectIssuesTab />} />
            <Route path="board" element={<ProjectBoardTab />} />
            <Route path="overview" element={<ProjectOverviewTab />} />
            <Route path="settings" element={<ProjectSettingsTab />} />
          </Route>
          <Route path="issues" element={<WorkspaceIssuesPage />} />
          <Route path="issues/:issueReference" element={<IssueDetailsPage />} />
          <Route path="my-issues" element={<WorkspaceIssuesPage mine />} />
          <Route path="members" element={<MembersPage />} />
          <Route path="settings" element={<WorkspaceSettingsPage />} />
          <Route index element={<WorkspaceRedirect />} />
        </Route>
      </Route>
    </Route>
    <Route path="*" element={<NotFoundPage />} />
  </Routes>
}

function App() {
  return <BrowserRouter><AuthProvider><AppRoutes /></AuthProvider></BrowserRouter>
}

export default App
