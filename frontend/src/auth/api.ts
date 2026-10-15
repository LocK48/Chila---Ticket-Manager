export type User = {
  id: string
  email: string
  displayName: string
  createdAt: string
}

export type RegistrationInput = {
  email: string
  password: string
  displayName: string
}

export type ApiErrorBody = {
  code?: string
  message?: string
  fieldErrors?: Record<string, string>
}

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(
    message: string,
    status: number,
    fieldErrors: Record<string, string> = {},
  ) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

function csrfCookie(): string | null {
  const cookie = document.cookie.split('; ').find((part) => part.startsWith('XSRF-TOKEN='))
  return cookie ? decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) : null
}

async function bootstrapCsrf(): Promise<void> {
  const response = await fetch('/api/v1/auth/csrf', { credentials: 'same-origin' })
  if (!response.ok) throw new ApiError('Could not prepare a secure request.', response.status)
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    if (!csrfCookie()) await bootstrapCsrf()
    const token = csrfCookie()
    if (!token) throw new ApiError('Could not prepare a secure request.', 0)
    headers.set('X-XSRF-TOKEN', token)
  }

  const response = await fetch(path, { ...init, method, headers, credentials: 'same-origin' })
  if (response.status === 204) return undefined as T

  const body = (await response.json().catch(() => ({}))) as ApiErrorBody & T
  if (!response.ok) {
    throw new ApiError(body.message ?? 'The request could not be completed.', response.status, body.fieldErrors)
  }
  return body
}

export async function register(input: RegistrationInput): Promise<User> {
  return request<User>('/api/v1/auth/register', { method: 'POST', body: JSON.stringify(input) })
}

export async function login(email: string, password: string): Promise<User> {
  const user = await request<User>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
  await bootstrapCsrf()
  return user
}

export async function currentUser(): Promise<User> {
  return request<User>('/api/v1/users/me')
}

export async function updateDisplayName(displayName: string): Promise<User> {
  return request<User>('/api/v1/users/me', {
    method: 'PATCH',
    body: JSON.stringify({ displayName }),
  })
}

export async function logout(): Promise<void> {
  await request<void>('/api/v1/auth/logout', { method: 'POST' })
}

export async function initializeSession(): Promise<User | null> {
  await bootstrapCsrf()
  try {
    return await currentUser()
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      try {
        await logout()
      } catch {
        // The next sign-in request can retry server-side cookie cleanup if the network was unavailable.
      }
      return null
    }
    throw error
  }
}
