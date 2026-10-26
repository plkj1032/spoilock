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
  getRooms: (options = {}) =>
    chatRequest("/api/chat/rooms", {
      ...options,
      cache: "no-store",
    }),

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
  reportMessage: async (roomId, messageId, reason, detail = "") => {
    try {
      return await chatRequest(
        `/api/chat/rooms/${roomId}/messages/${messageId}/reports`,
        {
          method: "POST",
          body: {
            reason,
            detail: detail.trim() || null,
          },
        },
      );
    } catch (error) {
      if (error instanceof ApiError) {
        const messages = {
          400: "신고 사유와 내용을 확인해 주세요. 자신의 메시지나 시스템 알림은 신고할 수 없습니다.",
          401: "로그인 후 다시 신고해 주세요.",
          403: "방장의 메시지는 신고할 수 없거나, 현재 신고 권한이 없습니다.",
          404: "채팅방 또는 신고할 메시지를 찾을 수 없습니다.",
          409: "이미 신고한 메시지입니다.",
        };

        if (messages[error.status]) {
          const reportError = new ApiError(
            error.status,
            messages[error.status],
          );

          reportError.cause = error;
          throw reportError;
        }
      }

      throw error;
    }
  },
};
