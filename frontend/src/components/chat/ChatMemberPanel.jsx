import { useEffect, useId, useRef } from "react";
import { createPortal } from "react-dom";

import FollowButton from "../common/FollowButton";

import "../../styles/ChatMemberPanel.css";
import "../../styles/ChatMemberActions.css";
import "../../styles/ChatMemberOwner.css";

export default function ChatMemberPanel({
  roomType,
  ownerUserId,
  members,
  loading,
  error,
  connected,
  currentUserId,
  canKick,
  kickingUserId,
  kickDisabled,
  kickError,
  bannedUserIds,
  onKick,
  currentUser,
  onClose,
  onReload,
  onOpenProfile,
}) {
  const dialogRef = useRef(null);
  const titleId = useId();

  // =============================================
  // 강퇴된 사용자 제외
  // =============================================
  const visibleMembers = members.filter(
    (member) => !bannedUserIds?.has(String(member.userId)),
  );

  // =============================================
  // 접속 중 / 미접속 사용자 분리
  // =============================================
  const onlineMembers = visibleMembers.filter((member) => member.online);
  const offlineMembers = visibleMembers.filter((member) => !member.online);

  const ready = connected && !loading && !error;
  const processing = kickingUserId != null;

  // =============================================
  // 모달 열기 및 포커스 복원
  // =============================================
  useEffect(() => {
    const dialog = dialogRef.current;
    const previousFocus = document.activeElement;

    if (dialog && !dialog.open) {
      dialog.showModal();
    }

    return () => {
      if (dialog?.open) {
        dialog.close();
      }

      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) {
        previousFocus.focus();
      }
    };
  }, []);

  function close() {
    if (!processing) {
      onClose();
    }
  }

  // =============================================
  // 참여자 프로필 열기
  // 부모 ChatRoomPage에서 모달 전환 처리
  // =============================================
  function openProfile(member) {
    onOpenProfile?.({
      userId: member.userId,
      nickname: member.nickname,
    });
  }

  // =============================================
  // 참여자 한 명 표시
  // =============================================
  function renderMember(member) {
    const mine = String(member.userId) === String(currentUserId);
    const isOwner =
      (roomType === "CUSTOM" || roomType === "PRIVATE") &&
      ownerUserId != null &&
      String(member.userId) === String(ownerUserId);
    const nickname = member.nickname || "사용자";
    const initial = Array.from(nickname.trim() || "?")[0];

    const kicking = String(kickingUserId) === String(member.userId);

    return (
      <li
        key={member.userId}
        className={`chat-member-item ${
          member.online ? "is-online" : "is-offline"
        }`}
      >
        {/* 프로필 클릭 영역 */}
        <button
          type="button"
          className="chat-member-profile-button"
          onClick={() => openProfile(member)}
          aria-label={`${nickname}님의 프로필 보기`}
        >
          {/* 아바타 */}
          <div className="chat-member-avatar" aria-hidden="true">
            {initial}

            {member.online && <span className="chat-member-avatar__dot" />}
          </div>

          {/* 닉네임 + 접속 여부 */}
          <div className="chat-member-info">
            <div className="chat-member-name">
              <strong>{nickname}</strong>

              {isOwner && (
                <span className="chat-member-owner">
                  <svg
                    viewBox="0 0 24 24"
                    width="13"
                    height="13"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="1.8"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    aria-hidden="true"
                  >
                    <path d="m3 6 4 4 5-6 5 6 4-4-3 12H6L3 6Z" />
                    <path d="M7 21h10" />
                  </svg>
                  방장
                </span>
              )}

              {mine && <span className="chat-member-me">나</span>}
            </div>

            <span className="chat-member-status">
              {member.online ? "현재 방에 접속 중" : "현재 방에 미접속"}
            </span>
          </div>
        </button>

        {/* 팔로우 버튼 */}
        {!mine && currentUser && (
          <div className="chat-member-follow">
            <FollowButton
              targetUserId={member.userId}
              currentUser={currentUser}
            />
          </div>
        )}

        {/* 방장 강퇴 버튼 */}
        {canKick && !mine && (
          <button
            type="button"
            className="chat-member-kick"
            disabled={processing || kickDisabled}
            aria-label={`${nickname}님 강퇴`}
            onClick={() => onKick(member.userId, nickname)}
          >
            {kicking ? "처리 중…" : "강퇴"}
          </button>
        )}
      </li>
    );
  }

  // =============================================
  // 참여자 모달
  // =============================================
  return createPortal(
    <dialog
      ref={dialogRef}
      className="chat-member-dialog"
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        close();
      }}
    >
      {/* 상단 */}
      <header className="chat-member-dialog__header">
        <div>
          <h2 id={titleId}>참여자</h2>

          <p>
            {ready
              ? `전체 ${visibleMembers.length}명 · 접속 중 ${onlineMembers.length}명`
              : "이 채팅방에 가입한 회원 목록"}
          </p>
        </div>

        <button
          type="button"
          className="chat-member-close"
          onClick={close}
          disabled={processing}
          aria-label="참여자 목록 닫기"
          autoFocus
        >
          ×
        </button>
      </header>

      {/* 본문 */}
      <div className="chat-member-dialog__body">
        {/* 강퇴 오류 */}
        {canKick && kickError && (
          <p className="chat-member-action-error" role="alert">
            {kickError}
          </p>
        )}

        {/* 연결 끊김 */}
        {!connected && (
          <p className="chat-member-notice" role="status">
            채팅에 다시 연결하면 참여자 목록을 확인할 수 있습니다.
          </p>
        )}

        {/* 로딩 */}
        {connected && loading && (
          <p className="chat-member-notice" role="status">
            참여자를 불러오는 중입니다.
          </p>
        )}

        {/* 오류 */}
        {connected && !loading && error && (
          <div className="chat-member-error" role="alert">
            <p>{error}</p>

            <button
              type="button"
              className="chat-member-retry"
              onClick={onReload}
            >
              다시 불러오기
            </button>
          </div>
        )}

        {/* 참여자 없음 */}
        {ready && visibleMembers.length === 0 && (
          <p className="chat-member-notice">현재 가입한 회원이 없습니다.</p>
        )}

        {/* 접속 중 */}
        {ready && onlineMembers.length > 0 && (
          <section className="chat-member-group">
            <h3>접속 중 · {onlineMembers.length}</h3>

            <ul>{onlineMembers.map(renderMember)}</ul>
          </section>
        )}

        {/* 미접속 */}
        {ready && offlineMembers.length > 0 && (
          <section className="chat-member-group">
            <h3>미접속 · {offlineMembers.length}</h3>

            <ul>{offlineMembers.map(renderMember)}</ul>
          </section>
        )}

        {/* 안내 */}
        {ready && visibleMembers.length > 0 && (
          <p className="chat-member-profile-hint">
            닉네임이나 프로필 이미지를 누르면 사용자 정보를 볼 수 있습니다.
          </p>
        )}
      </div>
    </dialog>,
    document.body,
  );
}
