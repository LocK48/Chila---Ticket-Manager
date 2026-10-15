import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../auth/api'
import { useAuth } from '../auth/useAuth'

export function HomePage() {
  const { user, logout, updateName } = useAuth()
  const navigate = useNavigate()
  const [displayName, setDisplayName] = useState(user?.displayName ?? '')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setMessage('')
    setError('')
    setBusy(true)
    try {
      await updateName(displayName)
      setMessage('Your profile has been updated.')
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Could not update your profile.')
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
          <p className="eyebrow">YOUR ACCOUNT</p>
          <h1>Good to have you here, {user?.displayName}.</h1>
          <p className="subtle">Your account is signed in and ready for the next step.</p>
        </div>
        <div className="avatar" aria-hidden="true">{user?.displayName.slice(0, 1).toUpperCase()}</div>
      </section>
      <section className="profile-card">
        <div className="section-heading">
          <div><h2>Profile</h2><p className="subtle">Manage the name your team sees.</p></div>
        </div>
        <p className="identity-label">Email address</p>
        <p className="identity-value">{user?.email}</p>
        <form onSubmit={saveProfile} className="profile-form">
          <label>Display name
            <input maxLength={80} required value={displayName} onChange={(event) => setDisplayName(event.target.value)} />
          </label>
          <button className="primary-button compact" disabled={busy}>{busy ? 'Saving…' : 'Save changes'}</button>
        </form>
        {message && <p className="notice" role="status">{message}</p>}
        {error && <p className="form-error" role="alert">{error}</p>}
      </section>
      <p className="home-footnote">Workspace and project tools will appear here as they are added.</p>
    </main>
  )
}
