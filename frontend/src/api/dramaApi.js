import { apiRequest } from "./authApi";

export const dramaApi = {
  // 공개된 드라마 목록
  getDramas: () => apiRequest("/api/dramas"),

  // 해당 드라마의 회차 목록
  getEpisodes: (dramaId) => apiRequest(`/api/dramas/${dramaId}/episodes`),

  // 로그인 사용자 시청 진도
  getProgress: (dramaId) =>
    apiRequest(`/api/me/progress/${dramaId}`, {
      retryOn401: true,
    }),

  // 시청 진도 변경
  updateProgress: (dramaId, watchedEpisodeNumber) =>
    apiRequest(`/api/me/progress/${dramaId}`, {
      method: "PUT",
      body: { watchedEpisodeNumber },
      retryOn401: true,
    }),
};
