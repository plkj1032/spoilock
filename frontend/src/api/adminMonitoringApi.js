import { apiRequest } from "./authApi";

export const adminMonitoringApi = {
  // 기존 채팅 통계
  chats: ({ range = "24h", dramaId = "", signal } = {}) => {
    const query = new URLSearchParams({ range });

    if (dramaId) {
      query.set("dramaId", String(dramaId));
    }

    return apiRequest(`/api/admin/monitoring/chats?${query}`, {
      signal,
      retryOn401: true,
      cache: "no-store",
    });
  },

  // 기존 확장 모니터링 / 메시지별 흐름 추적
  advanced: ({ range = "24h", dramaId = "", messageId = "", signal } = {}) => {
    const query = new URLSearchParams({ range });

    if (dramaId) {
      query.set("dramaId", String(dramaId));
    }

    const id = String(messageId ?? "").trim();

    if (id) {
      query.set("messageId", id);
    }

    return apiRequest(`/api/admin/monitoring/advanced?${query}`, {
      signal,
      retryOn401: true,
      cache: "no-store",
    });
  },

  // V24: 채팅 판정 전체 이력
  chatHistory: ({ page = 0, dramaId = "", signal } = {}) => {
    const query = new URLSearchParams({
      page: String(page),
    });

    if (dramaId) {
      query.set("dramaId", String(dramaId));
    }

    return apiRequest(`/api/admin/monitoring/chat-history?${query}`, {
      signal,
      retryOn401: true,
      cache: "no-store",
    });
  },

  // V24: 자막/영상 AI 분석 전체 이력
  jobHistory: ({ page = 0, dramaId = "", signal } = {}) => {
    const query = new URLSearchParams({
      page: String(page),
    });

    if (dramaId) {
      query.set("dramaId", String(dramaId));
    }

    return apiRequest(`/api/admin/monitoring/job-history?${query}`, {
      signal,
      retryOn401: true,
      cache: "no-store",
    });
  },
};
