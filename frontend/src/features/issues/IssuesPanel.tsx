import { useCallback, useEffect, useState, type FormEvent } from 'react'
import {
  ApiError, archiveIssue, createIssue, listIssues, restoreIssue, updateIssue,
  type Issue, type IssueInput, type IssuePriority, type IssueStatus, type IssueType,
  type WorkspaceMember,
} from '../../auth/api'

const statuses: IssueStatus[] = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE']
const priorities: IssuePriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']
const blank: IssueInput = { title: '', description: null, type: 'TASK', status: 'TODO', priority: 'MEDIUM', assigneeId: null, dueDate: null }

export function IssuesPanel({ projectId, members, canManage, active, projectActive }: {
  projectId: string; members: WorkspaceMember[]; canManage: boolean; active: boolean; projectActive: boolean
}) {
  const [issues, setIssues] = useState<Issue[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [priority, setPriority] = useState('')
  const [type, setType] = useState('')
  const [assigneeId, setAssigneeId] = useState('')
  const [includeArchived, setIncludeArchived] = useState(false)
  const [editing, setEditing] = useState('')
  const [draft, setDraft] = useState<IssueInput>(blank)
  const [creating, setCreating] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const load = useCallback(async () => {
    try {
      const result = await listIssues(projectId, { page, size: 10, search: search || undefined, status: (status || undefined) as IssueStatus | undefined, priority: (priority || undefined) as IssuePriority | undefined, type: (type || undefined) as IssueType | undefined, assigneeId: assigneeId && assigneeId !== 'unassigned' ? assigneeId : undefined, unassigned: assigneeId === 'unassigned' || undefined, includeArchived, sort: 'createdAt', direction: 'desc' })
      setIssues(result.items); setTotal(result.totalItems); setError('')
    } catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not load issues.') }
  }, [projectId, page, search, status, priority, type, assigneeId, includeArchived])
  useEffect(() => {
    if (active) void Promise.resolve().then(load)
  }, [active, load])

  function startEdit(issue: Issue) {
    setCreating(false); setEditing(issue.id)
    setDraft({ title: issue.title, description: issue.description, type: issue.type, status: issue.status, priority: issue.priority, assigneeId: issue.assigneeId, dueDate: issue.dueDate })
  }
  async function save(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError('')
    try {
      if (creating) await createIssue(projectId, draft)
      else if (editing) await updateIssue(editing, draft)
      setCreating(false); setEditing(''); setDraft(blank); await load()
    } catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not save this issue.') }
    finally { setBusy(false) }
  }
  async function archive(issue: Issue) {
    setBusy(true); setError('')
    try { await archiveIssue(issue.id); await load() }
    catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not archive this issue.') }
    finally { setBusy(false) }
  }
  async function restore(issue: Issue) {
    setBusy(true); setError('')
    try { await restoreIssue(issue.id); await load() }
    catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not restore this issue.') }
    finally { setBusy(false) }
  }

  return <section className="issues-panel" aria-label="Project issues">
    <div className="issues-heading"><div><h5>Issues</h5><p>{total} {total === 1 ? 'issue' : 'issues'}</p></div>
      {projectActive && <button className="text-action" onClick={() => { setCreating(true); setEditing(''); setDraft(blank) }}>New issue</button>}</div>
    <div className="issue-filters">
      <input aria-label="Search issues" placeholder="Search issues" value={search} onChange={(event) => { setPage(0); setSearch(event.target.value) }} />
      <select aria-label="Filter by status" value={status} onChange={(event) => { setPage(0); setStatus(event.target.value) }}><option value="">All statuses</option>{statuses.map((item) => <option key={item}>{item}</option>)}</select>
      <select aria-label="Filter by priority" value={priority} onChange={(event) => { setPage(0); setPriority(event.target.value) }}><option value="">All priorities</option>{priorities.map((item) => <option key={item}>{item}</option>)}</select>
      <select aria-label="Filter by type" value={type} onChange={(event) => { setPage(0); setType(event.target.value) }}><option value="">All types</option>{(['TASK', 'BUG', 'STORY'] as IssueType[]).map((item) => <option key={item}>{item}</option>)}</select>
      <select aria-label="Filter by assignee" value={assigneeId} onChange={(event) => { setPage(0); setAssigneeId(event.target.value) }}><option value="">Anyone</option><option value="unassigned">Unassigned</option>{members.map((member) => <option key={member.userId} value={member.userId}>{member.displayName}</option>)}</select>
      {canManage && <label className="issue-archived-toggle"><input type="checkbox" checked={includeArchived} onChange={(event) => setIncludeArchived(event.target.checked)} /> Archived</label>}
    </div>
    {error && <p className="form-error" role="alert">{error}</p>}
    {!projectActive && <p className="member-note">This project is archived. Issues are read-only until the project is active again.</p>}
    {(creating || editing) && <form className="issue-form" onSubmit={(event) => void save(event)}>
      <label>Title<input required maxLength={200} value={draft.title} onChange={(event) => setDraft({ ...draft, title: event.target.value })} /></label>
      <label>Description<textarea rows={2} maxLength={10000} value={draft.description ?? ''} onChange={(event) => setDraft({ ...draft, description: event.target.value || null })} /></label>
      <div className="issue-form-grid">
        <label>Type<select value={draft.type} onChange={(event) => setDraft({ ...draft, type: event.target.value as IssueType })}>{(['TASK', 'BUG', 'STORY'] as IssueType[]).map((item) => <option key={item}>{item}</option>)}</select></label>
        <label>Status<select value={draft.status} onChange={(event) => setDraft({ ...draft, status: event.target.value as IssueStatus })}>{statuses.map((item) => <option key={item}>{item}</option>)}</select></label>
        <label>Priority<select value={draft.priority} onChange={(event) => setDraft({ ...draft, priority: event.target.value as IssuePriority })}>{priorities.map((item) => <option key={item}>{item}</option>)}</select></label>
        <label>Assignee<select value={draft.assigneeId ?? ''} onChange={(event) => setDraft({ ...draft, assigneeId: event.target.value || null })}><option value="">Unassigned</option>{members.map((member) => <option key={member.userId} value={member.userId}>{member.displayName}</option>)}</select></label>
        <label>Due date<input type="date" value={draft.dueDate ?? ''} onChange={(event) => setDraft({ ...draft, dueDate: event.target.value || null })} /></label>
      </div>
      <div className="button-row"><button className="primary-button compact" disabled={busy}>{busy ? 'Saving…' : 'Save issue'}</button><button type="button" className="text-action" onClick={() => { setCreating(false); setEditing('') }}>Cancel</button></div>
    </form>}
    <div className="issue-list">{issues.map((issue) => <article className={`issue-row${issue.archivedAt ? ' issue-archived' : ''}`} key={issue.id}>
      <div className="issue-main"><div><span className={`status-pill issue-status-${issue.status.toLowerCase()}`}>{issue.status.replace('_', ' ').toLowerCase()}</span><span className={`priority-label priority-${issue.priority.toLowerCase()}`}>{issue.priority.toLowerCase()}</span></div><strong>{issue.title}</strong><small>{issue.type.toLowerCase()} · {issue.assigneeName ? `Assigned to ${issue.assigneeName}` : 'Unassigned'}{issue.dueDate ? ` · Due ${issue.dueDate}` : ''}</small></div>
      <div className="issue-actions">{!issue.archivedAt && projectActive && <button className="text-action" onClick={() => startEdit(issue)}>Edit</button>}{canManage && (issue.archivedAt ? <button className="text-action" disabled={busy || !projectActive} onClick={() => void restore(issue)}>Restore</button> : projectActive && <button className="remove-member-button" disabled={busy} onClick={() => void archive(issue)}>Archive</button>)}</div>
    </article>)}{!issues.length && <p className="empty-copy">No matching issues. Create one to start tracking work.</p>}</div>
    <div className="issue-pagination"><button className="secondary-button" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>Previous</button><span>Page {page + 1}</span><button className="secondary-button" disabled={(page + 1) * 10 >= total} onClick={() => setPage((current) => current + 1)}>Next</button></div>
  </section>
}
