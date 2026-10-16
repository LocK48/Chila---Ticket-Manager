import { useOutletContext } from "react-router-dom";
import type { Workspace } from "../auth/api";

export type AppLayoutContext = {
  workspaces: Workspace[];
  activeWorkspace: Workspace | null;
  refreshWorkspaces: () => Promise<Workspace[]>;
};

export function useAppLayout() {
  const context = useOutletContext<AppLayoutContext | undefined>();
  if (!context)
    throw new Error(
      "useAppLayout must be used within the authenticated app layout route.",
    );
  return context;
}
