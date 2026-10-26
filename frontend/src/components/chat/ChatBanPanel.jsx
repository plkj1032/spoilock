import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { chatApi } from "../../api/chatApi";
import "../../styles/ChatBanPanel.css";

export default function ChatBanPanel({
  roomId,
  bans,
  loading,
  error,
  onClose,
  onReleased,
  onReload,
}) {
  const dialogRef = useRef(null);
  const releasingRef = useRef(false);
  const [releasingId, setReleasingId] = useState(null);
  const [releaseError, setReleaseError] = useState("");

  useEffect(() => {
    const dialog = dialogRef.current;
    const previousFocus = document.activeElement;

    dialog.showModal();

    return () => {
      dialog.close();

      if (previousFocus instanceof HTMLElement) {
        previousFocus.focus();
      }
    };
  }, []);

  async function releaseBan(ban) {
    if (releasingRef.current) return;

    releasingRef.current = true;
    setReleasingId(ban.banId);
    setReleaseError("");

    try {
      await chatApi.releaseBan(roomId, ban.banId);
      onReleased(ban.banId);
    } catch (err) {
      setReleaseError(
        err.status === 403
          ? "강퇴를 해제할 권한이 없습니다."
          : "강퇴 해제에 실패했습니다. 다시 시도해 주세요.",
      );
    } finally {
      releasingRef.current = false;
      setReleasingId(null);
    }
  }

  return createPortal(
    <dialog
      ref={dialogRef}
      className="chat-ban-dialog"
      aria-labelledby="chat-ban-title"
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
    >
      <header className="chat-ban-dialog__header">
        <div>
          <h2 id="chat-ban-title">강퇴 관리</h2>
          <p>해제된 회원은 다시 입장할 수 있습니다.</p>
        </div>

        <button
          type="button"
          className="chat-ban-dialog__close"
          onClick={onClose}
          aria-label="강퇴 관리 닫기"
          autoFocus
        >
          ×
        </button>
      </header>

      <div className="chat-ban-dialog__body">
        {loading && <p role="status">목록을 불러오는 중입니다.</p>}

        {error && (
          <div role="alert">
            <p>{error}</p>
            <button
              type="button"
              className="chat-ban-release"
              onClick={onReload}
              disabled={loading}
            >
              다시 불러오기
            </button>
          </div>
        )}

        {releaseError && (
          <p className="chat-ban-error" role="alert">
            {releaseError}
          </p>
        )}

        {!loading && !error && bans.length === 0 && (
          <p className="chat-ban-empty">현재 강퇴된 회원이 없습니다.</p>
        )}

        {!loading &&
          !error &&
          bans.map((ban) => (
            <article key={ban.banId} className="chat-ban-item">
              <div className="chat-ban-item__top">
                <strong>{ban.nickname}</strong>

                <button
                  type="button"
                  className="chat-ban-release"
                  disabled={releasingId !== null}
                  onClick={() => releaseBan(ban)}
                >
                  {releasingId === ban.banId ? "해제 중…" : "강퇴 해제"}
                </button>
              </div>

              <p>{ban.reason || "입력된 사유 없음"}</p>

              <time dateTime={ban.createdAt}>
                {new Date(ban.createdAt).toLocaleString("ko-KR")}
              </time>
            </article>
          ))}
      </div>
    </dialog>,
    document.body,
  );
}
