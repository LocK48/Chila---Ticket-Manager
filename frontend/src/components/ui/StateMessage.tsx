import type { ReactNode } from "react";

export function LoadingState({
  children = "Loading…",
}: {
  children?: ReactNode;
}) {
  return (
    <div className="state-message" role="status">
      <span className="state-spinner" aria-hidden="true" />
      {children}
    </div>
  );
}

export function ErrorState({
  message,
  onRetry,
}: {
  message: string;
  onRetry?: () => void;
}) {
  return (
    <div className="state-message state-error" role="alert">
      <span>{message}</span>
      {onRetry && (
        <button className="secondary-button" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}

export function EmptyState({
  title,
  detail,
  action,
}: {
  title: string;
  detail?: string;
  action?: ReactNode;
}) {
  return (
    <div className="empty-state">
      <span className="empty-state-mark" aria-hidden="true">
        —
      </span>
      <h2>{title}</h2>
      {detail && <p>{detail}</p>}
      {action}
    </div>
  );
}
