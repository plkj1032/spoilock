import { apiRequest } from "./authApi";

const positionRequests = new Map();

function positionPath(dramaId, episodeId) {
  return `/api/watch/dramas/${dramaId}/episodes/${episodeId}/position`;
}

function enqueuePositionRequest(path, task) {
  const previous = positionRequests.get(path) ?? Promise.resolve();
  const result = previous.catch(() => {}).then(task);

  const settled = result.then(
    () => undefined,
    () => undefined,
  );

  positionRequests.set(path, settled);

  void settled.then(() => {
    if (positionRequests.get(path) === settled) {
      positionRequests.delete(path);
    }
  });

  return result;
}

export const dramaWatchApi = {
  getResume: async (dramaId, signal) => {
    const prefix = `/api/watch/dramas/${dramaId}/episodes/`;

    // 같은 작품의 진행 중인 저장 요청이 끝난 후 조회
    const pending = [...positionRequests.entries()]
      .filter(([path]) => path.startsWith(prefix))
      .map(([, request]) => request);

    await Promise.all(pending);

    return apiRequest(`/api/watch/dramas/${dramaId}/resume`, {
      retryOn401: true,
      cache: "no-store",
      signal,
    });
  },

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

  getPosition: (dramaId, episodeId, signal) => {
    const path = positionPath(dramaId, episodeId);

    return enqueuePositionRequest(path, () =>
      apiRequest(path, {
        retryOn401: true,
        cache: "no-store",
        signal,
      }),
    );
  },

  savePosition: (dramaId, episodeId, position) => {
    const path = positionPath(dramaId, episodeId);

    return enqueuePositionRequest(path, () =>
      apiRequest(path, {
        method: "PUT",
        body: position,
        cache: "no-store",
      }),
    );
  },
};
