import { apiRequest } from "./authApi";

export const dramaRatingApi = {
  get: (dramaId, signal) =>
    apiRequest(`/api/dramas/${dramaId}/rating`, {
      signal,
      cache: "no-store",
      retryOn401: true,
    }),

  save: (dramaId, score) =>
    apiRequest(`/api/dramas/${dramaId}/rating`, {
      method: "PUT",
      body: { score },
      retryOn401: true,
    }),

  remove: (dramaId) =>
    apiRequest(`/api/dramas/${dramaId}/rating`, {
      method: "DELETE",
      retryOn401: true,
    }),
};
