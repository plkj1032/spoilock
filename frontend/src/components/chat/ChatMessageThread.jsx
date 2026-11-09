import { useEffect, useRef, useState } from "react";
import "../../styles/ChatMessageThread.css";

// 같은 발신자·같은 분의 연속 메시지 최대 5개. 시스템 메시지는 묶음을 끊는다.
function groupChatMessages(messages) {
  let count = 0;
  return messages.map((message, index) => {
    const previous = messages[index - 1];
    const now = Date.parse(message.createdAt);
    const before = Date.parse(previous?.createdAt);
    const continuation = Boolean(
      message.messageType === "CHAT" &&
      previous?.messageType === "CHAT" &&
      message.userId != null &&
      previous.userId != null &&
      String(message.userId) === String(previous.userId) &&
      Number.isFinite(now) &&
      Number.isFinite(before) &&
      now >= before &&
      Math.floor(now / 60000) === Math.floor(before / 60000) &&
      count < 5,
    );
    count = message.messageType === "CHAT" ? (continuation ? count + 1 : 1) : 0;
    return { message, continuation };
  });
}

const PROTECTION = {
  ANALYZING: ["⏳", "스포일러 확인 중", "메시지를 분석하고 있어요."],
  NOT_WATCHED: ["🔒", "스포일러 보호 중", "아직 보지 않은 회차의 내용이에요."],
  REVIEW: ["🔒", "메시지 검토 중", "확인이 필요한 메시지를 보호하고 있어요."],
  FAILED: ["⏳", "확인 지연 중", "분석 상태를 확인하고 있어요."],
  HIDDEN: ["🚫", "숨겨진 메시지", "관리자가 숨긴 메시지예요."],
};

function ProtectedBubble({ reason }) {
  const [icon, title, description] = PROTECTION[reason] ?? [
    "🔒",
    "보호 중인 메시지",
    "아직 공개되지 않은 메시지예요.",
  ];
  return (
    <div className="talk-protection">
      <span className="talk-protection__icon" aria-hidden="true">
        {icon}
      </span>
      <div>
        <strong>{title}</strong>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function ChatMessageThread({
  messages,
  user,
  room,
  onlineUserIds,
  canKick,
  bannedUserIds,
  connected,
  kickingUserId,
  bansLoading,
  bansError,
  onProfile,
  onReport,
  onKick,
}) {
  const [menuId, setMenuId] = useState(null);
  const menuRef = useRef(null);
  const triggerRef = useRef(null);

  useEffect(() => {
    if (menuId == null) return;
    const closeOutside = (event) => {
      if (!menuRef.current?.contains(event.target)) setMenuId(null);
    };
    const closeEscape = (event) => {
      if (event.key === "Escape") {
        setMenuId(null);
        triggerRef.current?.focus();
      }
    };
    document.addEventListener("pointerdown", closeOutside);
    document.addEventListener("keydown", closeEscape);
    return () => {
      document.removeEventListener("pointerdown", closeOutside);
      document.removeEventListener("keydown", closeEscape);
    };
  }, [menuId]);

  return groupChatMessages(messages).map(({ message, continuation }) => {
    const blocked = message.visibility !== "SHOW";
    const mine = String(message.userId) === String(user.id);
    if (message.messageType !== "CHAT") {
      return (
        <div
          key={message.messageId}
          data-message-id={message.messageId}
          className="chat-system-message"
        >
          {blocked ? "공개되지 않은 알림입니다." : message.content}
        </div>
      );
    }
    const nickname = message.nickname || "사용자";
    const initial = Array.from(nickname.trim() || "?")[0];
    const owner =
      room?.createdById != null &&
      String(message.userId) === String(room.createdById);
    const reportable = Boolean(room && !mine && !owner);
    const kickable = Boolean(canKick && !mine);
    const banned = bannedUserIds.has(String(message.userId));
    const menuOpen = String(menuId) === String(message.messageId);
    const created = new Date(message.createdAt);
    const validTime = Number.isFinite(created.getTime());
    const profile = () => onProfile({ userId: message.userId, nickname });

    return (
      <article
        key={message.messageId}
        data-message-id={message.messageId}
        data-protected={blocked || undefined}
        className={`talk-row ${mine ? "talk-row--mine" : "talk-row--other"} ${continuation ? "talk-row--continued" : ""}`}
        aria-label={`${nickname}님의 메시지`}
      >
        {!mine && (
          <div className="talk-avatar-slot">
            {!continuation && (
              <button
                type="button"
                className="talk-avatar"
                onClick={profile}
                aria-label={`${nickname} 프로필 보기`}
              >
                <span aria-hidden="true">{initial}</span>
              </button>
            )}
          </div>
        )}
        <div className="talk-body">
          {!mine && !continuation && (
            <div className="talk-author">
              <button type="button" onClick={profile}>
                {nickname}
              </button>
              {onlineUserIds.has(String(message.userId)) && (
                <span
                  className="talk-online"
                  role="img"
                  aria-label="현재 접속 중"
                />
              )}
            </div>
          )}
          <div className="talk-line">
            <div
              className={`talk-bubble${blocked ? " talk-bubble--protected" : ""}`}
            >
              {blocked ? (
                <ProtectedBubble reason={message.blockReason} />
              ) : (
                <p className="talk-content">{message.content}</p>
              )}
            </div>
            <div className="talk-meta">
              {(reportable || kickable) && (
                <div className="talk-actions" ref={menuOpen ? menuRef : null}>
                  <button
                    type="button"
                    className="talk-menu-trigger"
                    aria-label={`${nickname}님의 메시지 옵션`}
                    aria-expanded={menuOpen}
                    onClick={(event) => {
                      triggerRef.current = event.currentTarget;
                      setMenuId(menuOpen ? null : message.messageId);
                    }}
                  >
                    ⋯
                  </button>
                  {menuOpen && (
                    <div className="talk-menu">
                      {reportable && (
                        <button
                          type="button"
                          onClick={() => {
                            setMenuId(null);
                            onReport(message);
                          }}
                        >
                          메시지 신고
                        </button>
                      )}
                      {kickable &&
                        (banned ? (
                          <span>강퇴된 사용자</span>
                        ) : (
                          <button
                            type="button"
                            disabled={
                              !connected ||
                              kickingUserId !== null ||
                              bansLoading ||
                              Boolean(bansError)
                            }
                            onClick={() => {
                              setMenuId(null);
                              onKick(message.userId, nickname);
                            }}
                          >
                            {String(kickingUserId) === String(message.userId)
                              ? "처리 중…"
                              : "사용자 강퇴"}
                          </button>
                        ))}
                    </div>
                  )}
                </div>
              )}
              <time
                dateTime={validTime ? message.createdAt : undefined}
                title={validTime ? created.toLocaleString("ko-KR") : undefined}
              >
                {validTime
                  ? created.toLocaleTimeString("ko-KR", {
                      hour: "2-digit",
                      minute: "2-digit",
                    })
                  : ""}
              </time>
            </div>
          </div>
        </div>
      </article>
    );
  });
}
