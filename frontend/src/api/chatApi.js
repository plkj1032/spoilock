import { apiRequest, ApiError } from "./authApi";

async function changeMembership(roomId, action) {
  try {
    return await apiRequest(`/api/chat/rooms/${roomId}/${action}`, {
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
  enterRoom: (roomId) => changeMembership(roomId, "enter"),

  leaveRoom: (roomId) => changeMembership(roomId, "leave"),
};
