import { useEffect, useState } from 'react'
import './App.css'

type HealthResponse = {
  status: string
  application: string
}

function App() {
  const [health, setHealth] = useState<HealthResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()

    fetch('/api/v1/health', { signal: controller.signal })
      .then((response) => {
        if (!response.ok) throw new Error(`Backend returned ${response.status}`)
        return response.json() as Promise<HealthResponse>
      })
      .then(setHealth)
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === 'AbortError') return
        setError(cause instanceof Error ? cause.message : 'Could not reach the backend')
      })

    return () => controller.abort()
  }, [])

  return (
    <main className="status-card">
      <p className="eyebrow">CHILA · FOUNDATION</p>
      <h1>Team work, in one place.</h1>
      <p className="intro">The project foundation is running. Next up: database design.</p>
      <section className="connection" aria-live="polite">
        <span className={`indicator ${health ? 'online' : error ? 'offline' : ''}`} />
        <div>
          <strong>Backend connection</strong>
          <p>
            {health
              ? `${health.application} · ${health.status}`
              : error ?? 'Checking the health endpoint…'}
          </p>
        </div>
      </section>
    </main>
  )
}

export default App
