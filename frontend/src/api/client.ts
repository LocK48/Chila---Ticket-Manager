import type { ApiErrorBody } from "./types";

export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;

  constructor(
    message: string,
    status: number,
    fieldErrors: Record<string, string> = {},
  ) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");
const apiUrl = (path: string) => `${apiBaseUrl}${path}`;
const credentialsMode: RequestCredentials = apiBaseUrl
  ? "include"
  : "same-origin";

function csrfCookie(): string | null {
  const cookie = document.cookie
    .split("; ")
    .find((part) => part.startsWith("XSRF-TOKEN="));
  return cookie ? decodeURIComponent(cookie.slice("XSRF-TOKEN=".length)) : null;
}

export async function bootstrapCsrf(): Promise<void> {
  const response = await fetch(apiUrl("/api/v1/auth/csrf"), {
    credentials: credentialsMode,
  });
  if (!response.ok)
    throw new ApiError("Could not prepare a secure request.", response.status);
}

export async function request<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type"))
    headers.set("Content-Type", "application/json");

  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    if (!csrfCookie()) await bootstrapCsrf();
    const token = csrfCookie();
    if (!token) throw new ApiError("Could not prepare a secure request.", 0);
    headers.set("X-XSRF-TOKEN", token);
  }

  const response = await fetch(apiUrl(path), {
    ...init,
    method,
    headers,
    credentials: credentialsMode,
  });
  if (response.status === 204) return undefined as T;

  const body = (await response.json().catch(() => ({}))) as ApiErrorBody & T;
  if (!response.ok) {
    throw new ApiError(
      body.message ?? "The request could not be completed.",
      response.status,
      body.fieldErrors,
    );
  }
  return body;
}
