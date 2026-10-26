import { apiRequest } from "./authApi";

export const notificationApi = {
  getList: (cursor = null, signal) =>
    apiRequest(
      cursor === null
        ? "/api/notifications"
        : `/api/notifications?cursor=${cursor}`,
      {
        retryOn401: true,
        cache: "no-store",
        signal,
      },
    ),

  getUnreadCount: (signal) =>
    apiRequest("/api/notifications/unread-count", {
      retryOn401: true,
      cache: "no-store",
      signal,
    }),

  markAsRead: (notificationId, signal) =>
    apiRequest(`/api/notifications/${notificationId}/read`, {
      method: "POST",
      retryOn401: true,
      signal,
    }),

  getReportReview: (notificationId, signal) =>
    apiRequest(`/api/notifications/${notificationId}/report-review`, {
      retryOn401: true,
      cache: "no-store",
      signal,
    }),

  decideReport: (notificationId, decision, reportIds, signal) =>
    apiRequest(`/api/notifications/${notificationId}/report-review`, {
      method: "POST",
      retryOn401: true,
      signal,
      body: {
        decision,
        reportIds,
      },
    }),
};
