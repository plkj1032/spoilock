import { apiRequest } from "./authApi";

function feedRequest(path, options = {}) {
  return apiRequest(path, {
    ...options,
    retryOn401: true,
  });
}

export const feedApi = {
  getFeed({ type = "ALL", cursor = null, size = 10, signal } = {}) {
    const params = new URLSearchParams({
      type,
      size: String(size),
    });

    if (cursor) params.set("cursor", cursor);

    return feedRequest(`/api/feed?${params}`, {
      signal,
      cache: "no-store",
    });
  },

  createPost(dramaId, content) {
    return feedRequest("/api/feed/posts", {
      method: "POST",
      body: { dramaId: Number(dramaId), content },
    });
  },

  // 좋아요 / 댓글
  getSocial(postId) {
    return feedRequest(`/api/feed/posts/${postId}/social`);
  },

  like(postId) {
    return feedRequest(`/api/feed/posts/${postId}/like`, {
      method: "POST",
    });
  },

  unlike(postId) {
    return feedRequest(`/api/feed/posts/${postId}/like`, {
      method: "DELETE",
    });
  },

  getComments(postId, page = 0) {
    return feedRequest(`/api/feed/posts/${postId}/comments?page=${page}`);
  },

  createComment(postId, content) {
    return feedRequest(`/api/feed/posts/${postId}/comments`, {
      method: "POST",
      body: { content },
    });
  },

  // 이미지 업로드 / 조회
  uploadMedia(postId, files) {
    const formData = new FormData();

    files.forEach((file) => {
      formData.append("files", file);
    });

    return feedRequest(`/api/feed/posts/${postId}/media`, {
      method: "POST",
      formData,
    });
  },

  getMedia(postId) {
    return feedRequest(`/api/feed/posts/${postId}/media`);
  },

  // 사용자별 피드
  getUserPreview(userId) {
    return feedRequest(`/api/feed/users/${userId}/preview`);
  },

  getUserFeed(userId, { cursor = null, size = 10, signal } = {}) {
    const params = new URLSearchParams({
      size: String(size),
    });

    if (cursor) params.set("cursor", cursor);

    return feedRequest(`/api/feed/users/${userId}?${params}`, {
      signal,
      cache: "no-store",
    });
  },
};
