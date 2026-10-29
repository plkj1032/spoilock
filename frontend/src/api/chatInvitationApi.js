import { apiRequest } from "./authApi";

function invitationRequest(path, options = {}) {
  return apiRequest(path, {
    ...options,
    retryOn401: true,
    cache: "no-store",
  });
}

export const chatInvitationApi = {
  getRooms: (signal) =>
    invitationRequest("/api/chat/invitations/rooms", {
      signal,
    }),
  getCandidates: (roomId, page = 0, signal) =>
    invitationRequest(
      `/api/chat/rooms/${roomId}/invitations/candidates?page=${page}`,
      { signal },
    ),

  send: (roomId, receiverUserId, signal) =>
    invitationRequest(`/api/chat/rooms/${roomId}/invitations`, {
      method: "POST",
      body: { receiverUserId },
      signal,
    }),

  accept: (invitationId, signal) =>
    invitationRequest(`/api/chat/invitations/${invitationId}/accept`, {
      method: "POST",
      signal,
    }),

  acceptByCode: (code, signal) =>
    invitationRequest("/api/chat/invitations/accept-by-code", {
      method: "POST",
      body: { code: code.trim() },
      signal,
    }),

  reject: (invitationId, signal) =>
    invitationRequest(`/api/chat/invitations/${invitationId}/reject`, {
      method: "POST",
      signal,
    }),
};
