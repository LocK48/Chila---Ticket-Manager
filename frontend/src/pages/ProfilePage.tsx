import { useState, type FormEvent } from 'react'
import { PageHeader } from '../components/ui/PageHeader'
import { useAuth } from '../auth/useAuth'

export function ProfilePage() {
  const { user, updateName } = useAuth(); const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const [notice, setNotice] = useState('')
  if (!user) return null
  async function save(event: FormEvent<HTMLFormElement>) { event.preventDefault(); const form = new FormData(event.currentTarget); setBusy(true); setError(''); try { await updateName(String(form.get('displayName')).trim()); setNotice('Profile updated.') } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not update your profile.') } finally { setBusy(false) } }
  return <><PageHeader eyebrow="ACCOUNT" title="My profile" description="Manage the profile information shown to your workspace." />{error && <p className="form-error" role="alert">{error}</p>}{notice && <p className="notice" role="status">{notice}</p>}<form className="workspace-form settings-form" onSubmit={(event) => void save(event)}><label>Display name<input name="displayName" required maxLength={100} defaultValue={user.displayName} /></label><label>Email address<input type="email" value={user.email} readOnly /></label><p className="member-note">Email address is managed by your account and cannot be changed here.</p><button className="primary-button compact" disabled={busy}>{busy ? 'Saving…' : 'Save profile'}</button></form><section className="detail-card profile-account-info"><h2>Account</h2><dl className="detail-grid"><div><dt>Member since</dt><dd>{new Date(user.createdAt).toLocaleDateString()}</dd></div></dl></section></>
}
