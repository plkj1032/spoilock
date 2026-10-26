import { useEffect, useId, useRef } from "react";
import { createPortal } from "react-dom";
import "../../styles/ChatMemberPanel.css";

export default function ChatMemberPanel({
  members,
  loading,
  error,
  connected,
  currentUserId,
  onClose,
  onReload,
}) {
  const dialogRef = useRef(null);
  const titleId = useId();

  const onlineMembers = members.filter((member) => member.online);
  const offlineMembers = members.filter((member) => !member.online);
  const ready = connected && !loading && !error;

  useEffect(() => {
    const dialog = dialogRef.current;
    const previousFocus = document.activeElement;

    dialog.showModal();

    return () => {
      dialog.close();

      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) {
        previousFocus.focus();
      }
    };
  }, []);

  function renderMember(member) {
    const mine = String(member.userId) === String(currentUserId);
    const initial = Array.from(member.nickname?.trim() || "?")[0];

    return (
      <li
        key={member.userId}
        className={`chat-member-item ${
          member.online ? "is-online" : "is-offline"
        }`}
      >
        <div className="chat-member-avatar" aria-hidden="true">
          {initial}

          {member.online && <span className="chat-member-avatar__dot" />}
        </div>

        <div className="chat-member-info">
          <div className="chat-member-name">
            <strong>{member.nickname}</strong>
            {mine && <span className="chat-member-me">나</span>}
          </div>

          <span className="chat-member-status">
            {member.online ? "현재 방에 접속 중" : "현재 방에 미접속"}
          </span>
        </div>
      </li>
    );
  }

  return createPortal(
    <dialog
      ref={dialogRef}
      className="chat-member-dialog"
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
    >
      <header className="chat-member-dialog__header">
        <div>
          <h2 id={titleId}>참여자</h2>

          <p>
            {ready
              ? `전체 ${members.length}명 · 접속 중 ${onlineMembers.length}명`
              : "이 채팅방에 가입한 회원 목록"}
          </p>
        </div>

        <button
          type="button"
          className="chat-member-close"
          onClick={onClose}
          aria-label="참여자 목록 닫기"
          autoFocus
        >
          ×
        </button>
      </header>

      <div className="chat-member-dialog__body">
        {!connected && (
          <p className="chat-member-notice" role="status">
            채팅에 다시 연결하면 참여자 목록을 확인할 수 있습니다.
          </p>
        )}

        {connected && loading && (
          <p className="chat-member-notice" role="status">
            참여자를 불러오는 중입니다.
          </p>
        )}

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

        {ready && members.length === 0 && (
          <p className="chat-member-notice">현재 가입한 회원이 없습니다.</p>
        )}

        {ready && onlineMembers.length > 0 && (
          <section className="chat-member-group">
            <h3>접속 중 · {onlineMembers.length}</h3>
            <ul>{onlineMembers.map(renderMember)}</ul>
          </section>
        )}

        {ready && offlineMembers.length > 0 && (
          <section className="chat-member-group">
            <h3>미접속 · {offlineMembers.length}</h3>
            <ul>{offlineMembers.map(renderMember)}</ul>
          </section>
        )}
      </div>
    </dialog>,
    document.body,
  );
}
