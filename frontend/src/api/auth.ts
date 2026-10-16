import { request, bootstrapCsrf, ApiError } from "./client";
import type { RegistrationInput, User } from "./types";

export async function register(input: RegistrationInput): Promise<User> {
  return request<User>("/api/v1/auth/register", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export async function requestRegistrationCode(email: string): Promise<void> {
  return request<void>("/api/v1/auth/registration-otp", {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

export async function requestPasswordChangeCode(): Promise<void> {
  return request<void>("/api/v1/users/me/password-otp", { method: "POST" });
}

export async function changePassword(
  verificationCode: string,
  newPassword: string,
): Promise<void> {
  return request<void>("/api/v1/users/me/password", {
    method: "PUT",
    body: JSON.stringify({ verificationCode, newPassword }),
  });
}

export async function login(email: string, password: string): Promise<User> {
  const user = await request<User>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
  await bootstrapCsrf();
  return user;
}

export async function currentUser(): Promise<User> {
  return request<User>("/api/v1/users/me");
}

export async function updateDisplayName(displayName: string): Promise<User> {
  return request<User>("/api/v1/users/me", {
    method: "PATCH",
    body: JSON.stringify({ displayName }),
  });
}

export async function logout(): Promise<void> {
  await request<void>("/api/v1/auth/logout", { method: "POST" });
}

export async function initializeSession(): Promise<User | null> {
  await bootstrapCsrf();
  try {
    return await currentUser();
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      try {
        await logout();
      } catch {
        // The next sign-in request can retry server-side cookie cleanup if the network was unavailable.
      }
      return null;
    }
    throw error;
  }
}
