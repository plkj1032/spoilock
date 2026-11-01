import { apiRequest } from "./authApi";

export const adminMonitoringApi = {
  chats: ({ range = "24h", dramaId = "", signal } = {}) => {
    const query = new URLSearchParams({ range });
    if (dramaId) query.set("dramaId", String(dramaId));
    return apiRequest(`/api/admin/monitoring/chats?${query}`, {
      signal,
      retryOn401: true,
      cache: "no-store",
    });
  },
  advanced: ({ range = "24h", dramaId = "", messageId = "", signal } = {}) => {
    const query = new URLSearchParams({ range });
    if (dramaId) query.set("dramaId", String(dramaId));
    if (messageId.trim()) query.set("messageId", messageId.trim());
    return apiRequest(`/api/admin/monitoring/advanced?${query}`, {
      signal,
      retryOn401: true,
      cache: "no-store",
    });
  },
};
