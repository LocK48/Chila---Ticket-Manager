import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError, getIssueReference, listProjects, listWorkspaceMembers, updateIssue, type Issue, type IssueInput, type Project, type WorkspaceMember } from '../auth/api'
import { IssueCollaboration } from '../features/issues/IssueCollaboration'
import { PageHeader } from '../components/ui/PageHeader'
import { ErrorState, LoadingState } from '../components/ui/StateMessage'
import { useAppLayout } from '../layouts/app-layout-context'

const allowedTransitions: Record<Issue['status'], Issue['status'][]> = {
  TODO: ['TODO', 'IN_PROGRESS'],
  IN_PROGRESS: ['TODO', 'IN_PROGRESS', 'IN_REVIEW'],
  IN_REVIEW: ['IN_PROGRESS', 'IN_REVIEW', 'DONE'],
  DONE: ['IN_PROGRESS', 'DONE'],
}

export function IssueDetailsPage() {
  const { workspaceId = '', issueReference = '' } = useParams()
  const { activeWorkspace } = useAppLayout()
  const [issue, setIssue] = useState<Issue | null>(null)
  const [project, setProject] = useState<Project | null>(null)
  const [members, setMembers] = useState<WorkspaceMember[]>([])
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState<IssueInput | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const load = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const [item, projects, workspaceMembers] = await Promise.all([getIssueReference(workspaceId, issueReference), listProjects(workspaceId), listWorkspaceMembers(workspaceId)])
      const foundProject = projects.find((candidate) => candidate.id === item.projectId)
      if (!foundProject) throw new Error('This issue is not available in the selected workspace.')
      setIssue(item); setProject(foundProject); setMembers(workspaceMembers)
    } catch (cause) { setError(cause instanceof ApiError ? cause.message : cause instanceof Error ? cause.message : 'Could not load this issue.') }
    finally { setLoading(false) }
  }, [issueReference, workspaceId])
  useEffect(() => { void Promise.resolve().then(load) }, [load])
  async function save(event: FormEvent) {
    event.preventDefault(); if (!draft || !issue) return
    setBusy(true); setError('')
    try { const updated = await updateIssue(issue.id, draft); setIssue(updated); setEditing(false) }
    catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not update this issue.') }
    finally { setBusy(false) }
  }
  if (loading) return <LoadingState>Loading issue…</LoadingState>
  if (error && !issue) return <><ErrorState message={error} onRetry={() => void load()} /><Link className="secondary-button" to={`/workspaces/${workspaceId}/issues`}>Back to issues</Link></>
  if (!issue || !project) return <p className="empty-copy">Issue unavailable.</p>
  const canEdit = Boolean(activeWorkspace)
  return <>
    <p className="breadcrumb"><Link to={`/workspaces/${workspaceId}/projects/${project.id}`}>{project.key}</Link><span>/</span>{issue.issueKey}</p>
    <PageHeader eyebrow={`${issue.issueKey} · ${issue.type.toLowerCase()}`} title={issue.title} description={`Reported by ${issue.reporterName} · Created ${new Date(issue.createdAt).toLocaleString()}`} action={!editing && canEdit && !issue.archivedAt && project.status === 'ACTIVE' ? <button className="secondary-button" onClick={() => { setDraft({ title: issue.title, description: issue.description, type: issue.type, status: issue.status, priority: issue.priority, assigneeId: issue.assigneeId, dueDate: issue.dueDate }); setEditing(true) }}>Edit issue</button> : undefined} />
    {error && <p className="form-error" role="alert">{error}</p>}
    {editing && draft ? <form className="workspace-form issue-detail-edit" onSubmit={(event) => void save(event)}><label>Title<input required maxLength={200} value={draft.title} onChange={(event) => setDraft({ ...draft, title: event.target.value })} /></label><label>Description<textarea rows={5} maxLength={10000} value={draft.description ?? ''} onChange={(event) => setDraft({ ...draft, description: event.target.value || null })} /></label><div className="issue-form-grid"><label>Type<select value={draft.type} onChange={(event) => setDraft({ ...draft, type: event.target.value as IssueInput['type'] })}>{['TASK', 'BUG', 'STORY'].map((value) => <option key={value}>{value}</option>)}</select></label><label>Status<select value={draft.status} onChange={(event) => setDraft({ ...draft, status: event.target.value as IssueInput['status'] })}>{allowedTransitions[issue.status].map((value) => <option key={value}>{value}</option>)}</select></label><label>Priority<select value={draft.priority} onChange={(event) => setDraft({ ...draft, priority: event.target.value as IssueInput['priority'] })}>{['LOW', 'MEDIUM', 'HIGH', 'URGENT'].map((value) => <option key={value}>{value}</option>)}</select></label><label>Assignee<select value={draft.assigneeId ?? ''} onChange={(event) => setDraft({ ...draft, assigneeId: event.target.value || null })}><option value="">Unassigned</option>{members.map((member) => <option key={member.userId} value={member.userId}>{member.displayName}</option>)}</select></label><label>Due date<input type="date" value={draft.dueDate ?? ''} onChange={(event) => setDraft({ ...draft, dueDate: event.target.value || null })} /></label></div><div className="button-row"><button className="primary-button compact" disabled={busy}>{busy ? 'Saving…' : 'Save changes'}</button><button type="button" className="text-action" onClick={() => setEditing(false)}>Cancel</button></div></form> : <section className="detail-card"><p className="issue-detail-description">{issue.description || 'No description provided.'}</p><dl className="detail-grid"><div><dt>Status</dt><dd>{issue.status.replace('_', ' ').toLowerCase()}</dd></div><div><dt>Priority</dt><dd>{issue.priority.toLowerCase()}</dd></div><div><dt>Assignee</dt><dd>{issue.assigneeName || 'Unassigned'}</dd></div><div><dt>Due date</dt><dd>{issue.dueDate || 'No due date'}</dd></div><div><dt>Reporter</dt><dd>{issue.reporterName}</dd></div><div><dt>Updated</dt><dd>{new Date(issue.updatedAt).toLocaleString()}</dd></div></dl></section>}
    <IssueCollaboration issueId={issue.id} writable={!issue.archivedAt && project.status === 'ACTIVE'} />
  </>
}
