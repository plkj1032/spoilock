import { apiRequest } from "./authApi";

export const dramaWatchApi = {
  getEpisodes: (dramaId, signal) =>
    apiRequest(`/api/watch/dramas/${dramaId}/episodes`, {
      retryOn401: true,
      cache: "no-store",
      signal,
    }),

  getPlayback: (dramaId, episodeId, signal) =>
    apiRequest(`/api/watch/dramas/${dramaId}/episodes/${episodeId}/playback`, {
      retryOn401: true,
      cache: "no-store",
      signal,
    }),
};
