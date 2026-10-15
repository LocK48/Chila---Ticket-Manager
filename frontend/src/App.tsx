import type { ReactNode } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { useAuth } from './auth/useAuth'
import { HomePage } from './pages/HomePage'
import { LoginPage, RegisterPage } from './pages/EntryPages'
import './App.css'

function ProtectedHome() {
  const { user, loading, error } = useAuth()
  if (loading) return <main className="loading-screen">Opening your workspace…</main>
  if (error) return <main className="loading-screen form-error" role="alert">{error}</main>
  return user ? <HomePage /> : <Navigate to="/login" replace />
}

function GuestOnly({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <main className="loading-screen">Opening your workspace…</main>
  return user ? <Navigate to="/" replace /> : children
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<ProtectedHome />} />
      <Route path="/login" element={<GuestOnly><LoginPage /></GuestOnly>} />
      <Route path="/register" element={<GuestOnly><RegisterPage /></GuestOnly>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
