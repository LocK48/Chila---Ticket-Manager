import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, listIssues, listProjects, type Issue, type Project } from '../auth/api'
import { PageHeader } from '../components/ui/PageHeader'
import { EmptyState, ErrorState, LoadingState } from '../components/ui/StateMessage'
import { useAppLayout } from '../layouts/app-layout-context'

export function OverviewPage() {
  const { activeWorkspace } = useAppLayout()
  const [projects, setProjects] = useState<Project[]>([])
  const [recentIssues, setRecentIssues] = useState<Issue[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const workspaceId = activeWorkspace?.id ?? ''
  const load = useCallback(async () => {
    if (!workspaceId) return
    setLoading(true); setError('')
    try {
      const projectItems = await listProjects(workspaceId)
      const pages = await Promise.all(projectItems.map((project) => listIssues(project.id, { page: 0, size: 100, sort: 'createdAt', direction: 'desc' })))
      const issues = pages.flatMap((page) => page.items)
        .sort((a, b) => Date.parse(b.updatedAt) - Date.parse(a.updatedAt)).slice(0, 6)
      setProjects(projectItems); setRecentIssues(issues)
    } catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not load this workspace overview.') }
    finally { setLoading(false) }
  }, [workspaceId])
  useEffect(() => { void Promise.resolve().then(load) }, [load])

  if (loading) return <LoadingState>Loading workspace overview…</LoadingState>
  if (error) return <ErrorState message={error} onRetry={() => void load()} />
  return <>
    <PageHeader eyebrow="WORKSPACE OVERVIEW" title={`Welcome to ${activeWorkspace?.name ?? 'your workspace'}`} description={activeWorkspace?.description || 'A shared space for your team’s projects and work.'}
      action={<Link className="primary-button compact" to={`/workspaces/${workspaceId}/projects`}>View projects</Link>} />
    <section className="overview-stats" aria-label="Workspace summary">
      <article className="overview-stat"><span className="overview-stat-icon">▦</span><div><strong>{projects.length}</strong><span>Projects</span></div></article>
      <article className="overview-stat"><span className="overview-stat-icon">◉</span><div><strong>{projects.filter((project) => project.status === 'ACTIVE').length}</strong><span>Active projects</span></div></article>
      <article className="overview-stat overview-stat-note"><span>Workspace members can see projects and issues according to their role.</span></article>
    </section>
    <section className="overview-section">
      <div className="section-heading"><div><h2>Projects</h2><p className="subtle">Projects in this workspace.</p></div><Link className="text-action" to={`/workspaces/${workspaceId}/projects`}>All projects</Link></div>
      {projects.length ? <div className="overview-project-grid">{projects.slice(0, 4).map((project) => <ProjectShortcut key={project.id} project={project} workspaceId={workspaceId} />)}</div>
        : <EmptyState title="No projects yet" detail="Create a project to start organizing work." action={<Link className="secondary-button" to={`/workspaces/${workspaceId}/projects`}>Create a project</Link>} />}
    </section>
    <section className="overview-section">
      <div className="section-heading"><div><h2>Recently updated issues</h2><p className="subtle">Ordered by update time among the 100 most recently created issues in each project.</p></div><Link className="text-action" to={`/workspaces/${workspaceId}/issues`}>Browse issues</Link></div>
      {recentIssues.length ? <div className="overview-issue-list">{recentIssues.map((issue) => <Link className="overview-issue-row" key={issue.id} to={`/workspaces/${workspaceId}/issues/${issue.issueKey}`}>
        <span className="project-key-mark">{issue.issueKey}</span><span className="overview-issue-title"><strong>{issue.title}</strong><small>{issue.type.toLowerCase()} · {issue.assigneeName || 'Unassigned'}</small></span>
        <span className={`status-pill issue-status-${issue.status.toLowerCase()}`}>{issue.status.replace('_', ' ').toLowerCase()}</span>
        <span className={`priority-label priority-${issue.priority.toLowerCase()}`}>{issue.priority.toLowerCase()}</span>
      </Link>)}</div> : <EmptyState title="No issues yet" detail="New project work will appear here as your team creates issues." />}
    </section>
  </>
}

function ProjectShortcut({ project, workspaceId }: { project: Project; workspaceId: string }) {
  return <Link className="overview-project" to={`/workspaces/${workspaceId}/projects/${project.id}`}>
    <span className="project-key-mark">{project.key}</span><span><strong>{project.name}</strong><small>{project.description || 'No description'}</small></span>
    <span className={`status-pill status-${project.status.toLowerCase()}`}>{project.status.toLowerCase()}</span>
  </Link>
}
