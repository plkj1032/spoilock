import { apiRequest } from "./authApi";

export const myPageApi = {
  getMyPage: (signal) =>
    apiRequest("/api/me/mypage?includeRooms=false", {
      retryOn401: true,
      cache: "no-store",
      signal,
    }),

  updateProfile: (nickname, signal) =>
    apiRequest("/api/me/profile", {
      method: "PATCH",
      body: { nickname },
      retryOn401: true,
      signal,
    }),
};
