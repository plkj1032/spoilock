import { apiRequest } from "./authApi";

function inviteLinkRequest(path, options = {}) {
  return apiRequest(path, {
    ...options,
    retryOn401: true,
    cache: "no-store",
  });
}

export const chatInviteLinkApi = {
  // 방장의 현재 공유 링크 상태 조회
  current: (roomId, signal) =>
    inviteLinkRequest(`/api/chat/rooms/${roomId}/invite-links`, { signal }),

  // 발급 또는 재발급
  issue: (roomId, maxUses) =>
    inviteLinkRequest(`/api/chat/rooms/${roomId}/invite-links`, {
      method: "POST",
      body: { maxUses },
    }),

  // 링크 취소
  revoke: (roomId, linkId) =>
    inviteLinkRequest(`/api/chat/rooms/${roomId}/invite-links/${linkId}`, {
      method: "DELETE",
    }),

  // 초대 내용 확인
  preview: (code, signal) =>
    inviteLinkRequest("/api/chat/invite-links/preview", {
      method: "POST",
      body: { code },
      signal,
    }),

  // 초대 수락
  accept: (code) =>
    inviteLinkRequest("/api/chat/invite-links/accept", {
      method: "POST",
      body: { code },
    }),
};

export function inviteLinkError(error) {
  const messages = {
    400: "초대코드 또는 초대 인원을 확인해 주세요.",
    401: "로그인 후 다시 시도해 주세요.",
    403: "강퇴된 회원이거나 초대 이용 권한이 없습니다.",
    404: "초대 링크 또는 이용 가능한 채팅방을 찾을 수 없습니다.",
    409: "이미 사용한 링크이거나 링크가 변경되었습니다. 새 초대를 확인해 주세요.",
    410: "만료·취소되었거나 사용 인원이 모두 찬 초대 링크입니다.",
  };

  return (
    messages[error.status] ||
    error.message ||
    "요청에 실패했습니다. 다시 시도해 주세요."
  );
}
