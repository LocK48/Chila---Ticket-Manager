import { useCallback, useEffect, useState, type FormEvent } from 'react'
import {
  ApiError, archiveIssue, createIssue, listIssues, restoreIssue, updateIssue,
  type Issue, type IssueInput, type IssuePriority, type IssueStatus, type IssueType,
  type WorkspaceMember,
} from '../../auth/api'
import { IssueCollaboration } from './IssueCollaboration'

const statuses: IssueStatus[] = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE']
const priorities: IssuePriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']
const allowedTransitions: Record<IssueStatus, IssueStatus[]> = {
  TODO: ['TODO', 'IN_PROGRESS'],
  IN_PROGRESS: ['TODO', 'IN_PROGRESS', 'IN_REVIEW'],
  IN_REVIEW: ['IN_PROGRESS', 'IN_REVIEW', 'DONE'],
  DONE: ['IN_PROGRESS', 'DONE'],
}
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
  const [sort, setSort] = useState('createdAt')
  const [direction, setDirection] = useState<'asc' | 'desc'>('desc')
  const [editing, setEditing] = useState('')
  const [expandedIssueIds, setExpandedIssueIds] = useState<Set<string>>(() => new Set())
  const [draft, setDraft] = useState<IssueInput>(blank)
  const [creating, setCreating] = useState(false)
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(false)
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')
  const load = useCallback(async () => {
    setLoading(true)
    try {
      const result = await listIssues(projectId, { page, size: 10, search: search || undefined, status: (status || undefined) as IssueStatus | undefined, priority: (priority || undefined) as IssuePriority | undefined, type: (type || undefined) as IssueType | undefined, assigneeId: assigneeId && assigneeId !== 'unassigned' ? assigneeId : undefined, unassigned: assigneeId === 'unassigned' || undefined, includeArchived, sort, direction })
      setIssues(result.items); setTotal(result.totalItems); setError('')
    } catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not load issues.') }
    finally { setLoading(false) }
  }, [projectId, page, search, status, priority, type, assigneeId, includeArchived, sort, direction])
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
      setNotice(creating ? 'Issue created.' : 'Issue updated.')
      setCreating(false); setEditing(''); setDraft(blank); await load()
    } catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not save this issue.') }
    finally { setBusy(false) }
  }
  async function archive(issue: Issue) {
    setBusy(true); setError('')
    try { await archiveIssue(issue.id); setNotice('Issue archived.'); await load() }
    catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not archive this issue.') }
    finally { setBusy(false) }
  }
  async function restore(issue: Issue) {
    setBusy(true); setError('')
    try { await restoreIssue(issue.id); setNotice('Issue restored.'); await load() }
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
      <select aria-label="Sort issues by" value={sort} onChange={(event) => setSort(event.target.value)}><option value="createdAt">Created</option><option value="dueDate">Due date</option><option value="priority">Priority</option><option value="status">Status</option><option value="title">Title</option></select>
      <select aria-label="Sort direction" value={direction} onChange={(event) => setDirection(event.target.value as 'asc' | 'desc')}><option value="desc">Descending</option><option value="asc">Ascending</option></select>
      {canManage && <label className="issue-archived-toggle"><input type="checkbox" checked={includeArchived} onChange={(event) => setIncludeArchived(event.target.checked)} /> Archived</label>}
    </div>
    {error && <p className="form-error" role="alert">{error}</p>}
    {notice && <p className="notice issue-notice" role="status">{notice}<button aria-label="Dismiss message" onClick={() => setNotice('')}>Dismiss</button></p>}
    {!projectActive && <p className="member-note">This project is archived. Issues are read-only until the project is active again.</p>}
    {(creating || editing) && <form className="issue-form" onSubmit={(event) => void save(event)}>
      <label>Title<input required maxLength={200} value={draft.title} onChange={(event) => setDraft({ ...draft, title: event.target.value })} /></label>
      <label>Description<textarea rows={2} maxLength={10000} value={draft.description ?? ''} onChange={(event) => setDraft({ ...draft, description: event.target.value || null })} /></label>
      <div className="issue-form-grid">
        <label>Type<select value={draft.type} onChange={(event) => setDraft({ ...draft, type: event.target.value as IssueType })}>{(['TASK', 'BUG', 'STORY'] as IssueType[]).map((item) => <option key={item}>{item}</option>)}</select></label>
        <label>Status<select value={draft.status} onChange={(event) => setDraft({ ...draft, status: event.target.value as IssueStatus })}>{(editing ? allowedTransitions[issues.find((issue) => issue.id === editing)?.status ?? draft.status] : statuses).map((item) => <option key={item}>{item}</option>)}</select></label>
        <label>Priority<select value={draft.priority} onChange={(event) => setDraft({ ...draft, priority: event.target.value as IssuePriority })}>{priorities.map((item) => <option key={item}>{item}</option>)}</select></label>
        <label>Assignee<select value={draft.assigneeId ?? ''} onChange={(event) => setDraft({ ...draft, assigneeId: event.target.value || null })}><option value="">Unassigned</option>{members.map((member) => <option key={member.userId} value={member.userId}>{member.displayName}</option>)}</select></label>
        <label>Due date<input type="date" value={draft.dueDate ?? ''} onChange={(event) => setDraft({ ...draft, dueDate: event.target.value || null })} /></label>
      </div>
      <div className="button-row"><button className="primary-button compact" disabled={busy}>{busy ? 'Saving…' : 'Save issue'}</button><button type="button" className="text-action" onClick={() => { setCreating(false); setEditing('') }}>Cancel</button></div>
    </form>}
    {(loading || !active) && <p className="subtle issue-loading" role="status">{active ? 'Loading issues…' : 'Loading workspace access…'}</p>}
    <div className="issue-list">{active && !loading && issues.map((issue) => <article className={`issue-row${issue.archivedAt ? ' issue-archived' : ''}`} key={issue.id}>
      <div className="issue-main"><div><span className={`status-pill issue-status-${issue.status.toLowerCase()}`}>{issue.status.replace('_', ' ').toLowerCase()}</span><span className={`priority-label priority-${issue.priority.toLowerCase()}`}>{issue.priority.toLowerCase()}</span></div><strong>{issue.title}</strong><small>{issue.type.toLowerCase()} · {issue.assigneeName ? `Assigned to ${issue.assigneeName}` : 'Unassigned'}{issue.dueDate ? ` · Due ${issue.dueDate}` : ''}</small></div>
      <div className="issue-actions">{!issue.archivedAt && projectActive && <button className="text-action" onClick={() => startEdit(issue)}>Edit</button>}{canManage && (issue.archivedAt ? <button className="text-action" disabled={busy || !projectActive} onClick={() => void restore(issue)}>Restore</button> : projectActive && <button className="remove-member-button" disabled={busy} onClick={() => void archive(issue)}>Archive</button>)}</div>
      <details className="issue-detail" onToggle={(event) => {
        const opened = event.currentTarget.open
        setExpandedIssueIds((current) => {
          const next = new Set(current)
          if (opened) next.add(issue.id)
          else next.delete(issue.id)
          return next
        })
      }}><summary>Issue details, conversation &amp; history</summary>{expandedIssueIds.has(issue.id) && <><IssueOverview issue={issue} /><IssueCollaboration issueId={issue.id} writable={!issue.archivedAt && projectActive} /></>}</details>
    </article>)}{active && !loading && !issues.length && <p className="empty-copy">No matching issues. Create one to start tracking work.</p>}</div>
    <div className="issue-pagination"><button className="secondary-button" disabled={page === 0 || loading || !active} onClick={() => setPage((current) => current - 1)}>Previous</button><span>Page {page + 1} of {Math.max(1, Math.ceil(total / 10))} · {total} total</span><button className="secondary-button" disabled={(page + 1) * 10 >= total || loading || !active} onClick={() => setPage((current) => current + 1)}>Next</button></div>
  </section>
}

function IssueOverview({ issue }: { issue: Issue }) {
  return <div className="issue-overview">
    <div className="issue-overview-title"><span>{issue.projectKey}</span><strong>{issue.title}</strong></div>
    <p>{issue.description || 'No description provided.'}</p>
    <dl><div><dt>Reported by</dt><dd>{issue.reporterName}</dd></div><div><dt>Assignee</dt><dd>{issue.assigneeName || 'Unassigned'}</dd></div>
      <div><dt>Type</dt><dd>{issue.type.toLowerCase()}</dd></div><div><dt>Priority</dt><dd>{issue.priority.toLowerCase()}</dd></div>
      <div><dt>Due date</dt><dd>{issue.dueDate || 'No due date'}</dd></div><div><dt>Created</dt><dd>{formatTimestamp(issue.createdAt)}</dd></div>
      <div><dt>Last updated</dt><dd>{formatTimestamp(issue.updatedAt)}</dd></div>
    </dl>
  </div>
}

function formatTimestamp(value: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
