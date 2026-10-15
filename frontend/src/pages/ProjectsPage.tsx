import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError, createProject, listProjects, updateProject, updateProjectStatus, type Project, type ProjectStatus } from '../auth/api'
import { PageHeader } from '../components/ui/PageHeader'
import { EmptyState, ErrorState, LoadingState } from '../components/ui/StateMessage'
import { useAppLayout } from '../layouts/app-layout-context'

export function ProjectsPage() {
  const { activeWorkspace } = useAppLayout()
  const navigate = useNavigate()
  const workspaceId = activeWorkspace?.id ?? ''
  const [projects, setProjects] = useState<Project[]>([])
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<'ALL' | ProjectStatus>('ALL')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState('')
  const canManage = activeWorkspace?.role !== 'MEMBER'
  const load = useCallback(async () => {
    if (!workspaceId) return
    setLoading(true); setError('')
    try { setProjects(await listProjects(workspaceId)) }
    catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not load projects.') }
    finally { setLoading(false) }
  }, [workspaceId])
  useEffect(() => { void Promise.resolve().then(load) }, [load])
  const visible = useMemo(() => projects.filter((project) => (status === 'ALL' || project.status === status) && `${project.name} ${project.key} ${project.description ?? ''}`.toLowerCase().includes(search.toLowerCase())), [projects, search, status])

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError('')
    const data = new FormData(event.currentTarget)
    try {
      const project = await createProject(workspaceId, String(data.get('name')).trim(), String(data.get('key')).trim().toUpperCase(), String(data.get('description') ?? '').trim())
      setShowForm(false); setNotice('Project created.'); await load()
      navigate(`/workspaces/${workspaceId}/projects/${project.id}`)
    } catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not create this project.') }
    finally { setBusy(false) }
  }
  async function toggleStatus(project: Project) {
    if (project.status === 'ACTIVE' && !window.confirm(`Archive ${project.name}? Issues will become read-only.`)) return
    setBusy(true); setError('')
    try { await updateProjectStatus(project.id, project.status === 'ACTIVE' ? 'ARCHIVED' : 'ACTIVE'); setNotice('Project status updated.'); await load() }
    catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not update project status.') }
    finally { setBusy(false) }
  }
  async function edit(event: FormEvent<HTMLFormElement>, projectId: string) {
    event.preventDefault(); const data = new FormData(event.currentTarget); setBusy(true); setError('')
    try { await updateProject(projectId, String(data.get('name')).trim(), String(data.get('description') ?? '').trim()); setEditingId(''); setNotice('Project updated.'); await load() }
    catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not update this project.') }
    finally { setBusy(false) }
  }
  if (loading) return <LoadingState>Loading projects…</LoadingState>
  return <>
    <PageHeader eyebrow="WORKSPACE" title="Projects" description="Browse and organize the work in this workspace." action={canManage ? <button className="primary-button compact" onClick={() => setShowForm((value) => !value)}>New project</button> : undefined} />
    {error && <ErrorState message={error} onRetry={() => void load()} />}{notice && <p className="notice" role="status">{notice}</p>}
    {showForm && canManage && <form className="workspace-form" onSubmit={(event) => void create(event)}><label>Project name<input name="name" required maxLength={100} /></label><label>Project key<input name="key" required minLength={2} maxLength={10} pattern="[A-Za-z][A-Za-z0-9]*" title="Start with a letter; use letters and numbers." /></label><label>Description<textarea name="description" rows={2} maxLength={500} /></label><div className="button-row"><button className="primary-button compact" disabled={busy}>{busy ? 'Creating…' : 'Create project'}</button><button type="button" className="text-action" onClick={() => setShowForm(false)}>Cancel</button></div></form>}
    <div className="page-toolbar"><input aria-label="Search projects" placeholder="Search projects" value={search} onChange={(event) => setSearch(event.target.value)} /><select aria-label="Filter projects by status" value={status} onChange={(event) => setStatus(event.target.value as typeof status)}><option value="ALL">All statuses</option><option value="ACTIVE">Active</option><option value="ARCHIVED">Archived</option></select></div>
    {visible.length ? <div className="project-page-grid">{visible.map((project) => <article className="project-page-card" key={project.id}>{editingId === project.id ? <form className="workspace-form project-edit-inline" onSubmit={(event) => void edit(event, project.id)}><label>Project name<input name="name" required maxLength={100} defaultValue={project.name} /></label><label>Description<textarea name="description" rows={2} maxLength={500} defaultValue={project.description ?? ''} /></label><div className="button-row"><button className="primary-button compact" disabled={busy}>Save changes</button><button className="text-action" type="button" onClick={() => setEditingId('')}>Cancel</button></div></form> : <><Link to={`/workspaces/${workspaceId}/projects/${project.id}`} className="project-card-main"><span className="project-key-mark">{project.key}</span><h2>{project.name}</h2><p>{project.description || 'No description provided.'}</p><span className={`status-pill status-${project.status.toLowerCase()}`}>{project.status.toLowerCase()}</span><small>Updated {new Date(project.updatedAt).toLocaleDateString()}</small></Link>{canManage && <div className="project-card-actions"><button className="text-action" disabled={busy || project.status !== 'ACTIVE'} onClick={() => setEditingId(project.id)}>Edit</button><button className="text-action" disabled={busy} onClick={() => void toggleStatus(project)}>{project.status === 'ACTIVE' ? 'Archive' : 'Restore'}</button></div>}</>}</article>)}</div> : <EmptyState title={search || status !== 'ALL' ? 'No matching projects' : 'No projects yet'} detail={search || status !== 'ALL' ? 'Try changing the search or status filter.' : 'Create a project to start organizing work.'} action={canManage && !showForm ? <button className="secondary-button" onClick={() => setShowForm(true)}>Create project</button> : undefined} />}
  </>
}
