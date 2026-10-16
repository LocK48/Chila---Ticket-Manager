import { useCallback, useEffect, useState, type DragEvent, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  ApiError, assignIssueToSprint, createIssue, listIssues, listSprintIssues, moveIssue,
  type Issue, type IssueInput, type IssuePage, type IssuePriority, type IssueStatus, type IssueType, type WorkspaceMember,
} from '../../auth/api'
import { ErrorState, LoadingState } from '../../components/ui/StateMessage'

const statuses: IssueStatus[] = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE']
const priorities: IssuePriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']
const types: IssueType[] = ['TASK', 'BUG', 'STORY']
const blankIssue: IssueInput = {
  title: '', description: null, type: 'TASK', status: 'TODO', priority: 'MEDIUM', assigneeId: null, dueDate: null,
}
type BoardColumns = Record<IssueStatus, Issue[]>
type ColumnInfo = Record<IssueStatus, Pick<IssuePage, 'page' | 'totalItems' | 'totalPages'>>
const emptyColumns = (): BoardColumns => ({ TODO: [], IN_PROGRESS: [], IN_REVIEW: [], DONE: [] })
const emptyInfo = (): ColumnInfo => ({
  TODO: { page: 0, totalItems: 0, totalPages: 0 }, IN_PROGRESS: { page: 0, totalItems: 0, totalPages: 0 },
  IN_REVIEW: { page: 0, totalItems: 0, totalPages: 0 }, DONE: { page: 0, totalItems: 0, totalPages: 0 },
})

export function KanbanBoard({ projectId, members, projectActive, sprintId }: {
  projectId: string; members: WorkspaceMember[]; projectActive: boolean; sprintId?: string
}) {
  const { workspaceId = '' } = useParams()
  const [columns, setColumns] = useState<BoardColumns>(emptyColumns)
  const [columnInfo, setColumnInfo] = useState<ColumnInfo>(emptyInfo)
  const [search, setSearch] = useState('')
  const [assigneeId, setAssigneeId] = useState('')
  const [priority, setPriority] = useState('')
  const [type, setType] = useState('')
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState<IssueStatus | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [draggingId, setDraggingId] = useState('')
  const [dragOverStatus, setDragOverStatus] = useState<IssueStatus | null>(null)
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [groupByAssignee, setGroupByAssignee] = useState(false)

  const fetchColumn = useCallback((status: IssueStatus, page: number) => sprintId
    ? listSprintIssues(projectId, sprintId, status, page, 50, {
      search: search.trim() || undefined,
      assigneeId: assigneeId && assigneeId !== 'unassigned' ? assigneeId : undefined,
      unassigned: assigneeId === 'unassigned' || undefined,
      priority: priority ? priority as IssuePriority : undefined,
      type: type ? type as IssueType : undefined,
    })
    : listIssues(projectId, {
    status, page, size: 50, sort: 'rank', direction: 'asc', search: search.trim() || undefined,
    assigneeId: assigneeId && assigneeId !== 'unassigned' ? assigneeId : undefined,
    unassigned: assigneeId === 'unassigned' || undefined,
    priority: priority ? priority as IssuePriority : undefined,
    type: type ? type as IssueType : undefined,
  }), [projectId, sprintId, search, assigneeId, priority, type])

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const results = await Promise.all(statuses.map((status) => fetchColumn(status, 0)))
      const nextColumns = emptyColumns(); const nextInfo = emptyInfo()
      statuses.forEach((status, index) => {
        nextColumns[status] = results[index].items
        nextInfo[status] = { page: results[index].page, totalItems: results[index].totalItems, totalPages: results[index].totalPages }
      })
      setColumns(nextColumns); setColumnInfo(nextInfo)
    } catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not load this board.') }
    finally { setLoading(false) }
  }, [fetchColumn])
  useEffect(() => { void Promise.resolve().then(load) }, [load])

  async function loadMore(status: IssueStatus) {
    setLoadingMore(status); setError('')
    try {
      const result = await fetchColumn(status, columnInfo[status].page + 1)
      setColumns((current) => ({ ...current, [status]: [...current[status], ...result.items] }))
      setColumnInfo((current) => ({ ...current, [status]: { page: result.page, totalItems: result.totalItems, totalPages: result.totalPages } }))
    } catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not load more issues.') }
    finally { setLoadingMore(null) }
  }

  async function move(issue: Issue, status: IssueStatus, beforeIssueId: string | null) {
    if (!projectActive || busy) return
    if (issue.status === status && beforeIssueId === issue.id) return
    setBusy(true); setError(''); setNotice('')
    try {
      await moveIssue(issue.id, status, beforeIssueId)
      await load()
      setNotice(`${issue.issueKey} moved to ${status.replace('_', ' ').toLowerCase()}.`)
    } catch (cause) {
      const message = cause instanceof ApiError ? cause.message : 'Could not move this issue.'
      await load()
      setError(message)
    } finally { setBusy(false); setDraggingId(''); setDragOverStatus(null) }
  }

  function onColumnDrop(event: DragEvent<HTMLElement>, status: IssueStatus) {
    event.preventDefault(); setDragOverStatus(null)
    const issue = findIssue(event.dataTransfer.getData('text/plain'))
    if (issue) void move(issue, status, null)
  }
  function onCardDrop(event: DragEvent<HTMLElement>, target: Issue, status: IssueStatus) {
    event.preventDefault(); event.stopPropagation(); setDragOverStatus(null)
    const issue = findIssue(event.dataTransfer.getData('text/plain'))
    if (issue && issue.id !== target.id) void move(issue, status, target.id)
  }
  function findIssue(id: string) {
    return statuses.flatMap((status) => columns[status]).find((issue) => issue.id === id)
  }
  function moveByKeyboard(issue: Issue, direction: 'up' | 'down') {
    const column = columns[issue.status]; const index = column.findIndex((item) => item.id === issue.id)
    if (direction === 'up' && index > 0) void move(issue, issue.status, column[index - 1].id)
    if (direction === 'down' && index >= 0 && index < column.length - 1) void move(issue, issue.status, column[index + 2]?.id ?? null)
  }

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const data = new FormData(event.currentTarget); setBusy(true); setError('')
    const input: IssueInput = {
      ...blankIssue,
      title: String(data.get('title')).trim(),
      description: String(data.get('description') ?? '').trim() || null,
      type: String(data.get('type')) as IssueType,
      priority: String(data.get('priority')) as IssuePriority,
      assigneeId: String(data.get('assigneeId') ?? '') || null,
      dueDate: String(data.get('dueDate') ?? '') || null,
    }
    try {
      const created = await createIssue(projectId, input)
      if (sprintId) await assignIssueToSprint(created.id, sprintId)
      setShowCreateForm(false); setNotice(`${created.issueKey} created.`); await load()
    } catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Could not create this issue.') }
    finally { setBusy(false) }
  }

  function renderCard(issue: Issue, status: IssueStatus) {
    return <BoardIssueCard key={issue.id} issue={issue} index={columns[status].indexOf(issue)}
      columnLength={columns[status].length} workspaceId={workspaceId} projectActive={projectActive}
      busy={busy} dragging={draggingId === issue.id} onDragStart={(id) => setDraggingId(id)}
      onDragEnd={() => { setDraggingId(''); setDragOverStatus(null) }} onDrop={(event, target) => onCardDrop(event, target, status)}
      onMove={(toStatus, beforeId) => void move(issue, toStatus, beforeId)}
      onMoveKeyboard={(direction) => moveByKeyboard(issue, direction)} />
  }

  if (loading) return <LoadingState>Loading Kanban board…</LoadingState>
  if (error && statuses.every((status) => columns[status].length === 0)) return <ErrorState message={error} onRetry={() => void load()} />
  return <section className="kanban-board" aria-label="Kanban board">
    <div className="board-toolbar">
      <div className="board-filters">
        <input aria-label="Search by issue key or title" placeholder="Search issues" value={search} onChange={(event) => setSearch(event.target.value)} />
        <select aria-label="Filter by assignee" value={assigneeId} onChange={(event) => setAssigneeId(event.target.value)}><option value="">Any assignee</option><option value="unassigned">Unassigned</option>{members.map((member) => <option value={member.userId} key={member.userId}>{member.displayName}</option>)}</select>
        <select aria-label="Filter by priority" value={priority} onChange={(event) => setPriority(event.target.value)}><option value="">Any priority</option>{priorities.map((value) => <option key={value}>{value}</option>)}</select>
        <select aria-label="Filter by issue type" value={type} onChange={(event) => setType(event.target.value)}><option value="">Any type</option>{types.map((value) => <option key={value}>{value}</option>)}</select>
        {!sprintId && <select aria-label="Group board by assignee" value={groupByAssignee ? 'assignee' : 'none'} onChange={(event) => setGroupByAssignee(event.target.value === 'assignee')}><option value="none">No swimlanes</option><option value="assignee">Group by assignee</option></select>}
        {(search || assigneeId || priority || type) && <button className="text-action" onClick={() => { setSearch(''); setAssigneeId(''); setPriority(''); setType('') }}>Clear filters</button>}
      </div>
      {projectActive && <button className="primary-button compact" disabled={busy} onClick={() => setShowCreateForm((value) => !value)}>Create issue</button>}
    </div>
    {error && <ErrorState message={error} onRetry={() => void load()} />}
    {notice && <p className="notice" role="status">{notice}</p>}
    {!projectActive && <p className="member-note">This project is archived. The board is read-only.</p>}
    {showCreateForm && <form className="board-create-form" onSubmit={(event) => void create(event)}><label>Title<input name="title" required maxLength={200} autoFocus /></label><label>Description<textarea name="description" rows={2} maxLength={10000} /></label><div className="board-create-fields"><label>Type<select name="type" defaultValue="TASK">{types.map((value) => <option key={value}>{value}</option>)}</select></label><label>Priority<select name="priority" defaultValue="MEDIUM">{priorities.map((value) => <option key={value}>{value}</option>)}</select></label><label>Assignee<select name="assigneeId" defaultValue=""><option value="">Unassigned</option>{members.map((member) => <option value={member.userId} key={member.userId}>{member.displayName}</option>)}</select></label><label>Due date<input type="date" name="dueDate" /></label></div><div className="button-row"><button className="primary-button compact" disabled={busy}>{busy ? 'Creating…' : 'Create issue'}</button><button type="button" className="text-action" onClick={() => setShowCreateForm(false)}>Cancel</button></div></form>}
    {!groupByAssignee ? <div className="kanban-columns">{statuses.map((status) => <section className={`kanban-column${dragOverStatus === status ? ' is-drop-target' : ''}`} key={status} onDragOver={(event) => { event.preventDefault(); setDragOverStatus(status) }} onDragLeave={(event) => { if (!event.currentTarget.contains(event.relatedTarget as Node)) setDragOverStatus(null) }} onDrop={(event) => onColumnDrop(event, status)} aria-label={`${status.replace('_', ' ')} issues`}>
      <ColumnHeading status={status} count={columnInfo[status].totalItems} /><div className="kanban-card-list">{columns[status].map((issue) => renderCard(issue, status))}
      {!columns[status].length && <p className="kanban-empty">{search || assigneeId || priority || type ? 'No matching issues.' : 'No issues in this column.'}</p>}
      {columnInfo[status].page + 1 < columnInfo[status].totalPages && <button className="kanban-load-more" disabled={loadingMore !== null} onClick={() => void loadMore(status)}>{loadingMore === status ? 'Loading…' : `Load more (${columns[status].length} of ${columnInfo[status].totalItems})`}</button>}
      </div></section>)}</div> : <div className="swimlane-board"><div className="swimlane-header"><span>Assignee</span>{statuses.map((status) => <ColumnHeading key={status} status={status} count={columns[status].length} />)}</div>
        {members.map((member) => ({ id: member.userId, name: member.displayName })).concat([{ id: 'unassigned', name: 'Unassigned' }])
          .filter((lane) => statuses.some((status) => columns[status].some((issue) => (issue.assigneeId ?? 'unassigned') === lane.id)))
          .map((lane) => <div className="swimlane-row" key={lane.id}><strong className="swimlane-label">{lane.name}</strong>{statuses.map((status) => <div key={status} className={`swimlane-cell${dragOverStatus === status ? ' is-drop-target' : ''}`} onDragOver={(event) => { event.preventDefault(); setDragOverStatus(status) }} onDrop={(event) => onColumnDrop(event, status)}>
            {columns[status].filter((issue) => (issue.assigneeId ?? 'unassigned') === lane.id).map((issue) => renderCard(issue, status))}
          </div>)}</div>)}
        {statuses.map((status) => columnInfo[status].page + 1 < columnInfo[status].totalPages && <button key={status} className="kanban-load-more" disabled={loadingMore !== null} onClick={() => void loadMore(status)}>Load more {status.replace('_', ' ').toLowerCase()} ({columns[status].length} of {columnInfo[status].totalItems})</button>)}
      </div>}
  </section>
}

function ColumnHeading({ status, count }: { status: IssueStatus; count: number }) {
  return <header className="kanban-column-heading"><h2><span className={`column-status-dot status-dot-${status.toLowerCase()}`} />{status.replace('_', ' ').toLowerCase()}</h2><span>{count}</span></header>
}

function BoardIssueCard({ issue, index, columnLength, workspaceId, projectActive, busy, dragging,
  onDragStart, onDragEnd, onDrop, onMove, onMoveKeyboard }: {
  issue: Issue; index: number; columnLength: number; workspaceId: string; projectActive: boolean
  busy: boolean; dragging: boolean; onDragStart: (id: string) => void; onDragEnd: () => void
  onDrop: (event: DragEvent<HTMLElement>, issue: Issue) => void
  onMove: (status: IssueStatus, beforeId: string | null) => void; onMoveKeyboard: (direction: 'up' | 'down') => void
}) {
  return <article className={`kanban-card${dragging ? ' is-dragging' : ''}`} draggable={projectActive && !busy} tabIndex={0}
    onDragStart={(event) => { if ((event.target as HTMLElement).closest('button, select, a, input, textarea')) { event.preventDefault(); return } event.dataTransfer.setData('text/plain', issue.id); event.dataTransfer.effectAllowed = 'move'; onDragStart(issue.id) }}
    onDragEnd={onDragEnd} onDragOver={(event) => event.preventDefault()} onDrop={(event) => onDrop(event, issue)}>
    <div className="kanban-card-meta"><Link to={`/workspaces/${workspaceId}/issues/${issue.issueKey}`}>{issue.issueKey}</Link><span className={`priority-label priority-${issue.priority.toLowerCase()}`}>{issue.priority.toLowerCase()}</span></div>
    <Link className="kanban-card-title" to={`/workspaces/${workspaceId}/issues/${issue.issueKey}`}>{issue.title}</Link>
    <div className="kanban-card-footer"><span className="kanban-card-type">{issue.type.toLowerCase()}</span>{issue.dueDate && <time dateTime={issue.dueDate}>{issue.dueDate}</time>}<span className="kanban-assignee" title={issue.assigneeName ?? 'Unassigned'}>{issue.assigneeName ? issue.assigneeName.split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase() : '—'}</span></div>
    {projectActive && <div className="kanban-card-controls"><label className="visually-hidden" htmlFor={`move-${issue.id}`}>Move {issue.issueKey} to status</label><select id={`move-${issue.id}`} aria-label={`Move ${issue.issueKey} to status`} value={issue.status} disabled={busy} onChange={(event) => onMove(event.target.value as IssueStatus, null)}>{statuses.map((value) => <option key={value}>{value}</option>)}</select><button type="button" aria-label={`Move ${issue.issueKey} up`} title="Move up" disabled={busy || index === 0} onClick={() => onMoveKeyboard('up')}>↑</button><button type="button" aria-label={`Move ${issue.issueKey} down`} title="Move down" disabled={busy || index === columnLength - 1} onClick={() => onMoveKeyboard('down')}>↓</button></div>}
  </article>
}
