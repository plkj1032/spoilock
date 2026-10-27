import { apiRequest } from "./authApi";

export const followApi = {
  // 팔로우
  follow(userId) {
    return apiRequest(`/api/users/${userId}/follow`, {
      method: "POST",
      retryOn401: true,
    });
  },

  // 언팔로우
  unfollow(userId) {
    return apiRequest(`/api/users/${userId}/follow`, {
      method: "DELETE",
      retryOn401: true,
    });
  },

  // 팔로워/팔로잉 수 및 팔로우 여부
  getStats(userId) {
    return apiRequest(`/api/users/${userId}/follow-stats`, {
      retryOn401: true,
    });
  },

  // 팔로워 목록
  getFollowers(userId, page = 0, size = 20) {
    return apiRequest(
      `/api/users/${userId}/followers?page=${page}&size=${size}`,
      {
        retryOn401: true,
      },
    );
  },

  // 팔로잉 목록
  getFollowing(userId, page = 0, size = 20) {
    return apiRequest(
      `/api/users/${userId}/following?page=${page}&size=${size}`,
      {
        retryOn401: true,
      },
    );
  },
};
