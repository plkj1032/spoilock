import { apiRequest } from "./authApi";

export const adminDramaApi = {
  getAll: () =>
    apiRequest("/api/admin/dramas", {
      retryOn401: true,
    }),

  getDetail: (id) =>
    apiRequest(`/api/admin/dramas/${id}`, {
      retryOn401: true,
    }),

  create: (data) =>
    apiRequest("/api/admin/dramas", {
      method: "POST",
      body: data,
      retryOn401: true,
    }),

  update: (id, data) =>
    apiRequest(`/api/admin/dramas/${id}`, {
      method: "PUT",
      body: data,
      retryOn401: true,
    }),

  publish: (id) =>
    apiRequest(`/api/admin/dramas/${id}/publish`, {
      method: "PATCH",
      retryOn401: true,
    }),

  hide: (id) =>
    apiRequest(`/api/admin/dramas/${id}/hide`, {
      method: "PATCH",
      retryOn401: true,
    }),
};
