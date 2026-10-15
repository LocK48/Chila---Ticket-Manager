import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  addWorkspaceMember,
  ApiError,
  createWorkspace,
  createProject,
  deleteWorkspace,
  listWorkspaceMembers,
  listWorkspaces,
  listProjects,
  removeWorkspaceMember,
  updateWorkspace,
  updateWorkspaceMemberRole,
  updateProject,
  updateProjectStatus,
  type MembershipRole,
  type Project,
  type ProjectStatus,
  type Workspace,
  type WorkspaceMember,
} from '../auth/api'
import { useAuth } from '../auth/useAuth'
import { IssuesPanel } from '../features/issues/IssuesPanel'

type EditableRole = Exclude<MembershipRole, 'OWNER'>

export function HomePage() {
  const { user, logout, updateName } = useAuth()
  const navigate = useNavigate()
  const [workspaces, setWorkspaces] = useState<Workspace[]>([])
  const [selectedId, setSelectedId] = useState('')
  const [members, setMembers] = useState<WorkspaceMember[]>([])
  const [membersWorkspaceId, setMembersWorkspaceId] = useState('')
  const [projects, setProjects] = useState<Project[]>([])
  const [projectsWorkspaceId, setProjectsWorkspaceId] = useState('')
  const [workspaceName, setWorkspaceName] = useState('')
  const [workspaceDescription, setWorkspaceDescription] = useState('')
  const [newWorkspaceName, setNewWorkspaceName] = useState('')
  const [newWorkspaceDescription, setNewWorkspaceDescription] = useState('')
  const [memberEmail, setMemberEmail] = useState('')
  const [memberRole, setMemberRole] = useState<EditableRole>('MEMBER')
  const [newProjectName, setNewProjectName] = useState('')
  const [newProjectKey, setNewProjectKey] = useState('')
  const [newProjectDescription, setNewProjectDescription] = useState('')
  const [editingProjectId, setEditingProjectId] = useState('')
  const [editingProjectName, setEditingProjectName] = useState('')
  const [editingProjectDescription, setEditingProjectDescription] = useState('')
  const [displayName, setDisplayName] = useState(user?.displayName ?? '')
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [loadingWorkspaces, setLoadingWorkspaces] = useState(true)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const selectedWorkspace = useMemo(
    () => workspaces.find((workspace) => workspace.id === selectedId) ?? null,
    [selectedId, workspaces],
  )
  const canManage = selectedWorkspace?.role === 'OWNER' || selectedWorkspace?.role === 'ADMIN'
  const isOwner = selectedWorkspace?.role === 'OWNER'

  useEffect(() => {
    let active = true
    listWorkspaces()
      .then((items) => {
        if (!active) return
        setWorkspaces(items)
        const firstWorkspace = items[0]
        setSelectedId(firstWorkspace?.id ?? '')
        setWorkspaceName(firstWorkspace?.name ?? '')
        setWorkspaceDescription(firstWorkspace?.description ?? '')
      })
      .catch((cause: unknown) => {
        if (active) setError(messageFor(cause, 'Could not load your workspaces.'))
      })
      .finally(() => {
        if (active) setLoadingWorkspaces(false)
      })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!selectedId) return
    let active = true
    listWorkspaceMembers(selectedId)
      .then((items) => {
        if (active) {
          setMembers(items)
          setMembersWorkspaceId(selectedId)
        }
      })
      .catch((cause: unknown) => {
        if (active) {
          setMembers([])
          setMembersWorkspaceId(selectedId)
          setError(messageFor(cause, 'Could not load workspace members.'))
        }
      })
    return () => { active = false }
  }, [selectedId])

  useEffect(() => {
    if (!selectedId) return
    let active = true
    listProjects(selectedId)
      .then((items) => {
        if (active) {
          setProjects(items)
          setProjectsWorkspaceId(selectedId)
        }
      })
      .catch((cause: unknown) => {
        if (active) {
          setProjects([])
          setProjectsWorkspaceId(selectedId)
          setError(messageFor(cause, 'Could not load workspace projects.'))
        }
      })
    return () => { active = false }
  }, [selectedId])

  const membersLoading = Boolean(selectedId) && membersWorkspaceId !== selectedId
  const projectsLoading = Boolean(selectedId) && projectsWorkspaceId !== selectedId

  function selectWorkspace(workspace: Workspace) {
    setSelectedId(workspace.id)
    setWorkspaceName(workspace.name)
    setWorkspaceDescription(workspace.description ?? '')
    setMessage('')
    setError('')
  }

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setMessage('')
    setError('')
    setBusy(true)
    try {
      await updateName(displayName)
      setMessage('Your profile has been updated.')
    } catch (cause) {
      setError(messageFor(cause, 'Could not update your profile.'))
    } finally {
      setBusy(false)
    }
  }

  async function createNewWorkspace(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setMessage('')
    setError('')
    setBusy(true)
    try {
      const created = await createWorkspace(newWorkspaceName.trim(), newWorkspaceDescription.trim())
      setWorkspaces((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name)))
      setSelectedId(created.id)
      setWorkspaceName(created.name)
      setWorkspaceDescription(created.description ?? '')
      setNewWorkspaceName('')
      setNewWorkspaceDescription('')
      setShowCreateForm(false)
      setMessage(`“${created.name}” is ready. You are its owner.`)
    } catch (cause) {
      setError(messageFor(cause, 'Could not create the workspace.'))
    } finally {
      setBusy(false)
    }
  }

  async function saveWorkspace(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedWorkspace) return
    setMessage('')
    setError('')
    setBusy(true)
    try {
      const updated = await updateWorkspace(selectedWorkspace.id, workspaceName.trim(), workspaceDescription.trim())
      setWorkspaces((current) => current.map((item) => item.id === updated.id ? updated : item))
      setMessage('Workspace details saved.')
    } catch (cause) {
      setError(messageFor(cause, 'Could not save workspace details.'))
    } finally {
      setBusy(false)
    }
  }

  async function inviteMember(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedWorkspace) return
    setMessage('')
    setError('')
    setBusy(true)
    try {
      const added = await addWorkspaceMember(selectedWorkspace.id, memberEmail.trim(), memberRole)
      setMembers((current) => [...current, added].sort((a, b) => a.joinedAt.localeCompare(b.joinedAt)))
      setMemberEmail('')
      setMessage(`${added.displayName} has been added to the workspace.`)
    } catch (cause) {
      setError(messageFor(cause, 'Could not add this person. Check that they already have a Chila account.'))
    } finally {
      setBusy(false)
    }
  }

  async function createNewProject(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedWorkspace) return
    setMessage('')
    setError('')
    setBusy(true)
    try {
      const created = await createProject(selectedWorkspace.id, newProjectName.trim(), newProjectKey.trim(), newProjectDescription.trim())
      setProjects((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name)))
      setNewProjectName('')
      setNewProjectKey('')
      setNewProjectDescription('')
      setMessage(`Project “${created.name}” was created.`)
    } catch (cause) {
      setError(messageFor(cause, 'Could not create the project.'))
    } finally {
      setBusy(false)
    }
  }

  async function saveProject(event: FormEvent<HTMLFormElement>, project: Project) {
    event.preventDefault()
    setMessage('')
    setError('')
    setBusy(true)
    try {
      const updated = await updateProject(project.id, editingProjectName.trim(), editingProjectDescription.trim())
      setProjects((current) => current.map((item) => item.id === updated.id ? updated : item)
        .sort((a, b) => a.name.localeCompare(b.name)))
      setEditingProjectId('')
      setMessage(`Project “${updated.name}” was updated.`)
    } catch (cause) {
      setError(messageFor(cause, 'Could not update the project.'))
    } finally {
      setBusy(false)
    }
  }

  async function changeProjectStatus(project: Project, status: ProjectStatus) {
    setMessage('')
    setError('')
    setBusy(true)
    try {
      const updated = await updateProjectStatus(project.id, status)
      setProjects((current) => current.map((item) => item.id === updated.id ? updated : item))
      setMessage(`Project “${updated.name}” is now ${status.toLowerCase()}.`)
    } catch (cause) {
      setError(messageFor(cause, 'Could not change the project status.'))
    } finally {
      setBusy(false)
    }
  }

  async function changeRole(member: WorkspaceMember, role: EditableRole) {
    if (!selectedWorkspace) return
    setMessage('')
    setError('')
    setBusy(true)
    try {
      const updated = await updateWorkspaceMemberRole(selectedWorkspace.id, member.userId, role)
      setMembers((current) => current.map((item) => item.userId === updated.userId ? updated : item))
      setMessage(`${updated.displayName} is now ${role.toLowerCase()}.`)
    } catch (cause) {
      setError(messageFor(cause, 'Could not change this member’s role.'))
    } finally {
      setBusy(false)
    }
  }

  async function removeMember(member: WorkspaceMember) {
    if (!selectedWorkspace) return
    setMessage('')
    setError('')
    setBusy(true)
    try {
      await removeWorkspaceMember(selectedWorkspace.id, member.userId)
      setMembers((current) => current.filter((item) => item.userId !== member.userId))
      if (member.userId === user?.id) {
        const remaining = workspaces.filter((item) => item.id !== selectedWorkspace.id)
        setWorkspaces(remaining)
        setSelectedId(remaining[0]?.id ?? '')
        setWorkspaceName(remaining[0]?.name ?? '')
        setWorkspaceDescription(remaining[0]?.description ?? '')
        setMessage('You left the workspace.')
      } else {
        setMessage(`${member.displayName} was removed.`)
      }
    } catch (cause) {
      setError(messageFor(cause, 'Could not remove this member.'))
    } finally {
      setBusy(false)
    }
  }

  async function removeWorkspace() {
    if (!selectedWorkspace || !window.confirm(`Delete “${selectedWorkspace.name}” and its memberships?`)) return
    setMessage('')
    setError('')
    setBusy(true)
    try {
      await deleteWorkspace(selectedWorkspace.id)
      const remaining = workspaces.filter((item) => item.id !== selectedWorkspace.id)
      setWorkspaces(remaining)
      setSelectedId(remaining[0]?.id ?? '')
      setWorkspaceName(remaining[0]?.name ?? '')
      setWorkspaceDescription(remaining[0]?.description ?? '')
      setMessage('Workspace deleted.')
    } catch (cause) {
      setError(messageFor(cause, 'Could not delete this workspace.'))
    } finally {
      setBusy(false)
    }
  }

  async function signOut() {
    try {
      await logout()
      navigate('/login', { replace: true })
    } catch {
      navigate('/login', {
        replace: true,
        state: { notice: 'Could not reach the server to clear your session. Please try signing in again.' },
      })
    }
  }

  return (
    <main className="home-shell">
      <header className="topbar">
        <Link className="brand" to="/">chila</Link>
        <button className="text-button" onClick={signOut}>Sign out</button>
      </header>
      <section className="welcome-panel">
        <div>
          <p className="eyebrow">YOUR TEAM SPACE</p>
          <h1>Good to have you here, {user?.displayName}.</h1>
          <p className="subtle">Create a workspace or pick one you belong to.</p>
        </div>
        <div className="avatar" aria-hidden="true">{user?.displayName.slice(0, 1).toUpperCase()}</div>
      </section>

      {error && <p className="form-error" role="alert">{error}</p>}
      {message && <p className="notice" role="status">{message}</p>}

      <section className="workspace-layout" aria-label="Workspace management">
        <aside className="workspace-sidebar">
          <div className="section-heading workspace-list-heading">
            <div><h2>Workspaces</h2><p className="subtle">Your teams in Chila</p></div>
            <button className="secondary-button" onClick={() => setShowCreateForm((visible) => !visible)}>
              {showCreateForm ? 'Cancel' : 'New'}
            </button>
          </div>
          {loadingWorkspaces ? <p className="subtle">Loading workspaces…</p> : workspaces.length === 0
            ? <p className="empty-copy">You haven’t joined a workspace yet.</p>
            : <div className="workspace-list" role="list">
              {workspaces.map((workspace) => (
                <button
                  className={`workspace-choice${workspace.id === selectedId ? ' is-selected' : ''}`}
                  key={workspace.id}
                  onClick={() => selectWorkspace(workspace)}
                  aria-current={workspace.id === selectedId ? 'true' : undefined}
                >
                  <span className="workspace-mark" aria-hidden="true">{workspace.name.slice(0, 1).toUpperCase()}</span>
                  <span className="workspace-choice-copy"><strong>{workspace.name}</strong><small>{workspace.role.toLowerCase()}</small></span>
                </button>
              ))}
            </div>}
          {showCreateForm && <form className="stacked-form create-workspace-form" onSubmit={createNewWorkspace}>
            <label>Workspace name
              <input required maxLength={100} value={newWorkspaceName} onChange={(event) => setNewWorkspaceName(event.target.value)} />
            </label>
            <label>Description <span className="field-hint">Optional</span>
              <input maxLength={500} value={newWorkspaceDescription} onChange={(event) => setNewWorkspaceDescription(event.target.value)} />
            </label>
            <button className="primary-button compact" disabled={busy}>{busy ? 'Creating…' : 'Create workspace'}</button>
          </form>}
        </aside>

        <section className="workspace-content">
          {!loadingWorkspaces && !selectedWorkspace ? <div className="empty-workspace">
            <span className="empty-icon" aria-hidden="true">＋</span>
            <h2>Start with a workspace</h2>
            <p className="subtle">Workspaces keep your team and projects together. Create one to get started.</p>
            {!showCreateForm && <button className="primary-button compact" onClick={() => setShowCreateForm(true)}>Create a workspace</button>}
          </div> : selectedWorkspace && <>
            <div className="workspace-title-row">
              <div><p className="eyebrow">WORKSPACE</p><h2>{selectedWorkspace.name}</h2></div>
              <span className="role-pill">{selectedWorkspace.role.toLowerCase()}</span>
            </div>
            {canManage && <form className="workspace-settings stacked-form" onSubmit={saveWorkspace}>
              <div className="section-heading"><div><h3>Workspace details</h3><p className="subtle">Update the name and description.</p></div></div>
              <label>Name
                <input required maxLength={100} value={workspaceName} onChange={(event) => setWorkspaceName(event.target.value)} />
              </label>
              <label>Description <span className="field-hint">Optional</span>
                <textarea maxLength={500} rows={3} value={workspaceDescription} onChange={(event) => setWorkspaceDescription(event.target.value)} />
              </label>
              <div className="button-row">
                <button className="primary-button compact" disabled={busy}>{busy ? 'Saving…' : 'Save details'}</button>
                {isOwner && <button className="danger-button" type="button" disabled={busy} onClick={removeWorkspace}>Delete workspace</button>}
              </div>
            </form>}

            <section className="project-section">
              <div className="section-heading">
                <div><h3>Projects</h3><p className="subtle">Projects belong to this workspace.</p></div>
              </div>
              <form className="project-create-form" onSubmit={createNewProject}>
                <label>Project name
                  <input required maxLength={120} placeholder="Website refresh" value={newProjectName} onChange={(event) => setNewProjectName(event.target.value)} />
                </label>
                <label>Project key
                  <input required maxLength={10} pattern="[A-Za-z][A-Za-z0-9]{1,9}" title="Use 2–10 letters and numbers, starting with a letter." placeholder="WEB" value={newProjectKey} onChange={(event) => setNewProjectKey(event.target.value.toUpperCase())} />
                </label>
                <label className="project-description-field">Description <span className="field-hint">Optional</span>
                  <input maxLength={2000} placeholder="What is this project for?" value={newProjectDescription} onChange={(event) => setNewProjectDescription(event.target.value)} />
                </label>
                <button className="primary-button compact" disabled={busy}>{busy ? 'Creating…' : 'Create project'}</button>
              </form>
              {projectsLoading ? <p className="subtle">Loading projects…</p> : <div className="project-list">
                {projects.map((project) => <article className="project-card" key={project.id}>
                  <div className="project-card-header">
                    <div className="project-key-mark">{project.key}</div>
                    <div className="project-card-title"><h4>{project.name}</h4><p>{project.description || 'No description yet.'}</p></div>
                    <span className={`status-pill status-${project.status.toLowerCase()}`}>{project.status.toLowerCase()}</span>
                  </div>
                  {canManage && <div className="project-card-actions">
                    <button className="text-action" onClick={() => {
                      setEditingProjectId(editingProjectId === project.id ? '' : project.id)
                      setEditingProjectName(project.name)
                      setEditingProjectDescription(project.description ?? '')
                    }}>{editingProjectId === project.id ? 'Cancel edit' : 'Edit details'}</button>
                    <label className="status-control">Status
                      <select value={project.status} disabled={busy} onChange={(event) => void changeProjectStatus(project, event.target.value as ProjectStatus)}>
                        <option value="ACTIVE">Active</option><option value="ARCHIVED">Archived</option>
                      </select>
                    </label>
                  </div>}
                  {editingProjectId === project.id && canManage && <form className="project-edit-form" onSubmit={(event) => void saveProject(event, project)}>
                    <label>Project name
                      <input required maxLength={120} value={editingProjectName} onChange={(event) => setEditingProjectName(event.target.value)} />
                    </label>
                    <label>Description
                      <textarea rows={2} maxLength={2000} value={editingProjectDescription} onChange={(event) => setEditingProjectDescription(event.target.value)} />
                    </label>
                    <button className="primary-button compact" disabled={busy}>{busy ? 'Saving…' : 'Save project'}</button>
                  </form>}
                  <IssuesPanel projectId={project.id} members={members} canManage={canManage} active={membersWorkspaceId === selectedId && !membersLoading} projectActive={project.status === 'ACTIVE'} />
                </article>)}
                {!projects.length && <p className="empty-copy">No projects yet. Add one to get the workspace started.</p>}
              </div>}
            </section>

            <section className="member-section">
              <div className="section-heading">
                <div><h3>People</h3><p className="subtle">{members.length} {members.length === 1 ? 'person' : 'people'} in this workspace</p></div>
              </div>
              {canManage && <form className="add-member-form" onSubmit={inviteMember}>
                <label className="member-email-field">Add a person by email
                  <input required type="email" maxLength={320} placeholder="name@example.com" value={memberEmail} onChange={(event) => setMemberEmail(event.target.value)} />
                </label>
                <label className="role-field">Role
                  <select value={memberRole} onChange={(event) => setMemberRole(event.target.value as EditableRole)}>
                    <option value="MEMBER">Member</option>
                    <option value="ADMIN">Admin</option>
                  </select>
                </label>
                <button className="primary-button compact" disabled={busy}>{busy ? 'Adding…' : 'Add person'}</button>
              </form>}
              {!canManage && <p className="member-note">Ask a workspace owner or admin to add people.</p>}
              {membersLoading ? <p className="subtle">Loading people…</p> : <div className="member-list">
                {members.map((member) => {
                  const isSelf = member.userId === user?.id
                  const canEdit = canManage && member.role !== 'OWNER' && (isOwner || member.role !== 'ADMIN')
                  return <article className="member-row" key={member.userId}>
                    <span className="member-avatar" aria-hidden="true">{member.displayName.slice(0, 1).toUpperCase()}</span>
                    <div className="member-identity"><strong>{member.displayName}{isSelf ? ' (you)' : ''}</strong><span>{member.email}</span></div>
                    <span className={`role-pill role-${member.role.toLowerCase()}`}>{member.role.toLowerCase()}</span>
                    {canEdit && <label className="visually-hidden" htmlFor={`role-${member.userId}`}>Role for {member.displayName}</label>}
                    {canEdit && <select
                      id={`role-${member.userId}`}
                      aria-label={`Role for ${member.displayName}`}
                      value={member.role}
                      disabled={busy}
                      onChange={(event) => void changeRole(member, event.target.value as EditableRole)}
                    ><option value="MEMBER">Member</option><option value="ADMIN">Admin</option></select>}
                    {member.role !== 'OWNER' && (canEdit || isSelf) && <button
                      className="remove-member-button"
                      disabled={busy}
                      onClick={() => void removeMember(member)}
                    >{isSelf ? 'Leave' : 'Remove'}</button>}
                  </article>
                })}
                {!members.length && !membersLoading && <p className="empty-copy">No members found.</p>}
              </div>}
            </section>
          </>}
        </section>
      </section>

      <section className="profile-card account-card">
        <div className="section-heading"><div><h2>Profile</h2><p className="subtle">Manage the name your team sees.</p></div></div>
        <p className="identity-label">Email address</p>
        <p className="identity-value">{user?.email}</p>
        <form onSubmit={saveProfile} className="profile-form">
          <label>Display name
            <input maxLength={80} required value={displayName} onChange={(event) => setDisplayName(event.target.value)} />
          </label>
          <button className="primary-button compact" disabled={busy}>Save changes</button>
        </form>
      </section>
      <p className="home-footnote">Workspace access is managed by your role. Only existing Chila users can be added at this stage.</p>
    </main>
  )
}

function messageFor(cause: unknown, fallback: string): string {
  if (cause instanceof ApiError) return cause.message || fallback
  return fallback
}
