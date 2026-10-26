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

  requestAnalysis(episodeId) {
    return apiRequest(`/api/admin/episodes/${episodeId}/analysis/request`, {
      method: "POST",
      retryOn401: true,
    });
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

  reviewEvent(eventId, verified) {
    return apiRequest(`/api/admin/spoiler-events/${eventId}/review`, {
      method: "PATCH",
      body: { verified },
      retryOn401: true,
    });
  },
};
