import { useCallback, useEffect, useState } from "react";
import { ApiError } from "../../api/client";
import { listNotifications, markNotificationRead } from "../../api/notifications";
import { type Notification } from "../../api/types";

export function NotificationsPanel() {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<Notification[]>([]);
  const [unread, setUnread] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [all, unreadPage] = await Promise.all([
        listNotifications(),
        listNotifications(true),
      ]);
      setItems(all.items);
      setUnread(unreadPage.totalItems);
      setError("");
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not load notifications.",
      );
    } finally {
      setLoading(false);
    }
  }, []);
  useEffect(() => {
    void Promise.resolve().then(load);
  }, [load]);
  async function markRead(notification: Notification) {
    if (notification.readAt) return;
    try {
      const updated = await markNotificationRead(notification.id);
      setItems((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      );
      setUnread((count) => Math.max(0, count - 1));
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? cause.message
          : "Could not update this notification.",
      );
    }
  }
  return (
    <div className="notifications-wrap">
      <button
        className="notification-trigger"
        aria-expanded={open}
        onClick={() => {
          setOpen((value) => !value);
          if (!open) void load();
        }}
      >
        Notifications
        {unread > 0 && (
          <span className="notification-count">
            {unread > 99 ? "99+" : unread}
          </span>
        )}
      </button>
      {open && (
        <section className="notifications-popover" aria-label="Notifications">
          <div className="notifications-title">
            <strong>Notifications</strong>
            <button className="text-action" onClick={() => void load()}>
              Refresh
            </button>
          </div>
          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}
          {loading && (
            <p className="subtle" role="status">
              Loading notifications…
            </p>
          )}
          {!loading &&
            items.map((item) => (
              <button
                className={`notification-item${item.readAt ? "" : " notification-unread"}`}
                key={item.id}
                onClick={() => void markRead(item)}
              >
                <span className="notification-indicator" />
                <span>
                  <strong>{item.message}</strong>
                  <small>
                    {formatDate(item.createdAt)}
                    {item.readAt ? " · Read" : " · Mark read"}
                  </small>
                </span>
              </button>
            ))}
          {!loading && !items.length && !error && (
            <p className="empty-copy">You’re all caught up.</p>
          )}
        </section>
      )}
    </div>
  );
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
