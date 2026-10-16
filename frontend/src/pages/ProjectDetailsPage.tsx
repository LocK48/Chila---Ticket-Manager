import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, NavLink, Outlet, useOutletContext, useParams } from 'react-router-dom'
import { ApiError, listProjects, listWorkspaceMembers, updateProject, type Project, type WorkspaceMember } from '../auth/api'
import { IssuesPanel } from '../features/issues/IssuesPanel'
import { KanbanBoard } from '../features/issues/KanbanBoard'
import { BacklogView, ActiveSprintView } from '../features/sprints/SprintPages'
import { PageHeader } from '../components/ui/PageHeader'
import { ErrorState, LoadingState } from '../components/ui/StateMessage'
import { useAppLayout } from '../layouts/app-layout-context'

export function ProjectDetailsPage() {
  const { workspaceId = '', projectId = '' } = useParams()
  const { activeWorkspace } = useAppLayout()
  const [project, setProject] = useState<Project | null>(null)
  const [members, setMembers] = useState<WorkspaceMember[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const load = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const [projects, workspaceMembers] = await Promise.all([listProjects(workspaceId), listWorkspaceMembers(workspaceId)])
      setProject(projects.find((item) => item.id === projectId) ?? null); setMembers(workspaceMembers)
    } catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not load this project.') }
    finally { setLoading(false) }
  }, [projectId, workspaceId])
  useEffect(() => { void Promise.resolve().then(load) }, [load])
  if (loading) return <LoadingState>Loading project…</LoadingState>
  if (error) return <ErrorState message={error} onRetry={() => void load()} />
  if (!project) return <section className="access-state"><h1>Project unavailable</h1><p>This project was not found in the selected workspace.</p><Link className="secondary-button" to={`/workspaces/${workspaceId}/projects`}>Back to projects</Link></section>
  const canManage = activeWorkspace?.role !== 'MEMBER'
  return <>
    <p className="breadcrumb"><Link to={`/workspaces/${workspaceId}/projects`}>Projects</Link><span>/</span>{project.key}</p>
    <PageHeader eyebrow={project.key} title={project.name} description={project.description || 'No project description.'} action={canManage ? <Link className="secondary-button" to={`/workspaces/${workspaceId}/projects/${project.id}/settings`}>Project settings</Link> : undefined} />
    <nav className="project-tabs" aria-label="Project pages"><NavLink end to={`/workspaces/${workspaceId}/projects/${project.id}`}>Issues</NavLink><NavLink to={`/workspaces/${workspaceId}/projects/${project.id}/board`}>Board</NavLink><NavLink to={`/workspaces/${workspaceId}/projects/${project.id}/backlog`}>Backlog</NavLink><NavLink to={`/workspaces/${workspaceId}/projects/${project.id}/active-sprint`}>Active sprint</NavLink><NavLink to={`/workspaces/${workspaceId}/projects/${project.id}/overview`}>Overview</NavLink>{canManage && <NavLink to={`/workspaces/${workspaceId}/projects/${project.id}/settings`}>Settings</NavLink>}</nav>
    <Outlet context={{ project, members, canManage, setProject }} />
  </>
}

export function ProjectIssuesTab() {
  const { project, members, canManage } = useProjectContext()
  return <IssuesPanel projectId={project.id} members={members} canManage={canManage} active projectActive={project.status === 'ACTIVE'} />
}

export function ProjectOverviewTab() {
  const { project } = useProjectContext()
  return <section className="detail-card"><h2>Project overview</h2><dl className="detail-grid"><div><dt>Key</dt><dd>{project.key}</dd></div><div><dt>Status</dt><dd>{project.status.toLowerCase()}</dd></div><div><dt>Created</dt><dd>{new Date(project.createdAt).toLocaleDateString()}</dd></div><div><dt>Last updated</dt><dd>{new Date(project.updatedAt).toLocaleDateString()}</dd></div></dl></section>
}

export function ProjectBoardTab() {
  const { project, members } = useProjectContext()
  return <KanbanBoard projectId={project.id} members={members} projectActive={project.status === 'ACTIVE'} />
}

export function ProjectBacklogRoute() {
  const { project, members, canManage } = useProjectContext()
  return <BacklogView project={project} members={members} canManage={canManage} projectActive={project.status === 'ACTIVE'} />
}
export function ProjectActiveSprintRoute() {
  const { project, members, canManage } = useProjectContext()
  return <ActiveSprintView project={project} members={members} canManage={canManage} projectActive={project.status === 'ACTIVE'} />
}

export function ProjectSettingsTab() {
  const { workspaceId = '' } = useParams()
  const { project, canManage, setProject } = useProjectContext()
  const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const [notice, setNotice] = useState('')
  async function save(event: FormEvent<HTMLFormElement>) { event.preventDefault(); const data = new FormData(event.currentTarget); setBusy(true); setError(''); try { setProject(await updateProject(project.id, String(data.get('name')).trim(), String(data.get('description') ?? '').trim())); setNotice('Project settings saved.') } catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not update project settings.') } finally { setBusy(false) } }
  if (!canManage) return <section className="access-state"><h2>Project settings are restricted</h2><p>Ask a workspace admin or owner to update this project.</p></section>
  return <><form className="workspace-form settings-form" onSubmit={(event) => void save(event)}><h2>Project settings</h2>{error && <p className="form-error" role="alert">{error}</p>}{notice && <p className="notice" role="status">{notice}</p>}<label>Project name<input name="name" required maxLength={100} defaultValue={project.name} key={project.updatedAt} /></label><label>Project key<input value={project.key} readOnly /></label><label>Description<textarea name="description" rows={4} maxLength={500} defaultValue={project.description ?? ''} key={`${project.id}-${project.updatedAt}`} /></label><p className="member-note">Project key is permanent. To archive or restore this project, use the projects list.</p><button className="primary-button compact" disabled={busy}>{busy ? 'Saving…' : 'Save changes'}</button></form><Link className="secondary-button" to={`/workspaces/${workspaceId}/projects`}>Back to projects</Link></>
}

function useProjectContext() {
  return useOutletContext<{ project: Project; members: WorkspaceMember[]; canManage: boolean; setProject: (project: Project) => void }>()
}
