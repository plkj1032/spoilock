import { apiRequest, ApiError } from "./authApi";

function chatRequest(path, options = {}) {
  return apiRequest(path, {
    ...options,
    retryOn401: true,
  });
}

async function changeMembership(roomId, action) {
  try {
    return await chatRequest(`/api/chat/rooms/${roomId}/${action}`, {
      method: "POST",
    });
  } catch (error) {
    if (error instanceof ApiError && error.status === 403) {
      throw new ApiError(
        403,
        "요청이 거절되었습니다. 방 이용 권한이나 인증 상태를 확인해 주세요.",
      );
    }

    throw error;
  }
}

export const chatApi = {
  getRooms: () => chatRequest("/api/chat/rooms"),

  getRoom: (roomId) => chatRequest(`/api/chat/rooms/${roomId}`),
  getMembers: (roomId, options = {}) =>
    chatRequest(`/api/chat/rooms/${roomId}/members`, {
      ...options,
      cache: "no-store",
    }),
  createRoom: (dramaId, roomName, description) =>
    chatRequest("/api/chat/rooms", {
      method: "POST",
      body: {
        dramaId,
        roomName,
        description,
      },
    }),

  enterRoom: (roomId) => changeMembership(roomId, "enter"),

  leaveRoom: (roomId) => changeMembership(roomId, "leave"),

  kickMember: (roomId, userId, reason) =>
    chatRequest(`/api/chat/rooms/${roomId}/kick`, {
      method: "POST",
      body: { userId, reason },
    }),
  getBans: (roomId) => chatRequest(`/api/chat/rooms/${roomId}/bans`),

  releaseBan: (roomId, banId) =>
    chatRequest(`/api/chat/rooms/${roomId}/bans/${banId}/release`, {
      method: "POST",
    }),
};
