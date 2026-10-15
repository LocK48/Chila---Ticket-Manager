import { useState, type FormEvent, type ReactNode } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ApiError, register } from '../auth/api'
import { useAuth } from '../auth/useAuth'

function EntryLayout({ children }: { children: ReactNode }) {
  return (
    <main className="entry-shell">
      <section className="entry-card">
        <Link className="brand" to="/">chila</Link>
        {children}
      </section>
      <p className="entry-caption">A calmer place to move team work forward.</p>
    </main>
  )
}

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const notice = (location.state as { notice?: string } | null)?.notice

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      await login(email, password)
      navigate('/', { replace: true })
    } catch (cause) {
      setError(cause instanceof ApiError && cause.status === 401
        ? 'Email or password is incorrect.'
        : cause instanceof Error ? cause.message : 'Could not sign in.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <EntryLayout>
      <p className="eyebrow">WELCOME BACK</p>
      <h1>Sign in to your workspace</h1>
      <p className="subtle">Pick up where your team left off.</p>
      {notice && <p className="notice" role="status">{notice}</p>}
      {error && <p className="form-error" role="alert">{error}</p>}
      <form onSubmit={submit} className="form-stack">
        <label>Email address
          <input type="email" autoComplete="email" required value={email} onChange={(event) => setEmail(event.target.value)} />
        </label>
        <label>Password
          <input type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} />
        </label>
        <button className="primary-button" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
      </form>
      <p className="form-footer">New to Chila? <Link to="/register">Create an account</Link></p>
    </EntryLayout>
  )
}

export function RegisterPage() {
  const navigate = useNavigate()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      await register({ displayName, email, password })
      navigate('/login', { replace: true, state: { notice: 'Your account is ready. Sign in to continue.' } })
    } catch (cause) {
      if (cause instanceof ApiError && cause.fieldErrors.email) setError(cause.fieldErrors.email)
      else if (cause instanceof ApiError && cause.fieldErrors.password) setError(cause.fieldErrors.password)
      else if (cause instanceof ApiError && cause.fieldErrors.displayName) setError(cause.fieldErrors.displayName)
      else setError(cause instanceof Error ? cause.message : 'Could not create your account.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <EntryLayout>
      <p className="eyebrow">GET STARTED</p>
      <h1>Create your account</h1>
      <p className="subtle">Bring your team's projects and work together.</p>
      {error && <p className="form-error" role="alert">{error}</p>}
      <form onSubmit={submit} className="form-stack">
        <label>Your name
          <input autoComplete="name" maxLength={80} required value={displayName} onChange={(event) => setDisplayName(event.target.value)} />
        </label>
        <label>Email address
          <input type="email" autoComplete="email" maxLength={320} required value={email} onChange={(event) => setEmail(event.target.value)} />
        </label>
        <label>Password
          <input type="password" autoComplete="new-password" minLength={12} required value={password} onChange={(event) => setPassword(event.target.value)} />
          <span className="field-hint">At least 12 characters.</span>
        </label>
        <button className="primary-button" disabled={busy}>{busy ? 'Creating account…' : 'Create account'}</button>
      </form>
      <p className="form-footer">Already have an account? <Link to="/login">Sign in</Link></p>
    </EntryLayout>
  )
}
