import { createContext } from "react";
import type { User } from "../api/types";

export type AuthContextValue = {
  user: User | null;
  loading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
  updateName: (displayName: string) => Promise<void>;
};

export const AuthContext = createContext<AuthContextValue | null>(null);
