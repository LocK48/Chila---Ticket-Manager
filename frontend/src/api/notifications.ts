import { request } from "./client";
import type { Notification, PageResult } from "./types";

export async function listNotifications(
  unreadOnly = false,
): Promise<PageResult<Notification>> {
  return request<PageResult<Notification>>(
    `/api/v1/users/me/notifications?size=20&unreadOnly=${unreadOnly}`,
  );
}

export async function markNotificationRead(
  notificationId: string,
): Promise<Notification> {
  return request<Notification>(
    `/api/v1/notifications/${encodeURIComponent(notificationId)}/read`,
    { method: "PATCH" },
  );
}
