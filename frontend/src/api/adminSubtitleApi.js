import { apiRequest } from "./authApi";

export const adminSubtitleApi = {
  getAnalysis(episodeId) {
    return apiRequest(`/api/admin/episodes/${episodeId}/analysis`, {
      retryOn401: true,
    });
  },

  upload(episodeId, file) {
    const formData = new FormData();
    formData.append("file", file);

    return apiRequest(`/api/admin/episodes/${episodeId}/subtitles`, {
      method: "POST",
      formData,
      retryOn401: true,
    });
  },

  // replace: 이미 사건이 있는 회차 재분석 (검수·수동 사건은 보존)
  // withVideo: 같은 회차에 등록된 MP4 화면도 함께 분석 (자막 + 영상)
  requestAnalysis(episodeId, { replace = false, withVideo = false } = {}) {
    const query = new URLSearchParams({
      replace: String(replace),
      withVideo: String(withVideo),
    });

    return apiRequest(
      `/api/admin/episodes/${episodeId}/analysis/request?${query}`,
      {
        method: "POST",
        retryOn401: true,
      },
    );
  },

  getEvents(episodeId) {
    return apiRequest(`/api/admin/episodes/${episodeId}/spoiler-events`, {
      retryOn401: true,
    });
  },

  createManualEvent(episodeId, eventSummary) {
    return apiRequest(`/api/admin/episodes/${episodeId}/spoiler-events`, {
      method: "POST",
      body: { eventSummary },
      retryOn401: true,
    });
  },

  // verificationStatus: VERIFIED(승인, firstRevealEpisode 필수) / REJECTED(반려) / PENDING(대기로)
  reviewEvent(eventId, verificationStatus, firstRevealEpisode = null) {
    return apiRequest(
      `/api/admin/spoiler-events/${encodeURIComponent(eventId)}/review`,
      {
        method: "PATCH",
        body: {
          verificationStatus,
          ...(firstRevealEpisode ? { firstRevealEpisode } : {}),
        },
        retryOn401: true,
      },
    );
  },
};
