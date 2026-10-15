import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import {
  currentUser,
  initializeSession,
  login as loginRequest,
  logout as logoutRequest,
  updateDisplayName,
  type User,
} from './api'
import { AuthContext, type AuthContextValue } from './auth-context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setError(null)
    setUser(await currentUser())
  }, [])

  useEffect(() => {
    let active = true
    initializeSession()
      .then((current) => {
        if (active) setUser(current)
      })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : 'Could not connect to Chila.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loading,
      error,
      login: async (email, password) => {
        setError(null)
        setUser(await loginRequest(email, password))
      },
      logout: async () => {
        try {
          await logoutRequest()
        } finally {
          setUser(null)
        }
      },
      refresh,
      updateName: async (displayName) => {
        setUser(await updateDisplayName(displayName))
      },
    }),
    [user, loading, error, refresh],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
