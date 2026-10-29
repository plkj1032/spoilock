import { useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { chatInvitationApi } from "../../api/chatInvitationApi";
import "../../styles/ChatFollowingInviteModal.css";

const STATUS_LABELS = {
  AVAILABLE: "초대 보내기",
  INVITED: "초대 완료",
  JOINED: "참여 중",
  BANNED: "초대 불가",
  UNAVAILABLE: "초대 불가",
};

export default function ChatFollowingInviteModal({
  roomId,
  roomName,
  onClose,
  onEnter,
}) {
  const dialogRef = useRef(null);
  const activeRef = useRef(false);
  const busyRef = useRef(false);
  const titleId = useId();
  const codeId = useId();

  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [revision, setRevision] = useState(0);
  const [loading, setLoading] = useState(true);
  const [sendingId, setSendingId] = useState(null);
  const [error, setError] = useState("");
  const [sent, setSent] = useState(null);
  const [copyMessage, setCopyMessage] = useState("");

  useEffect(() => {
    activeRef.current = true;
    const dialog = dialogRef.current;
    const previousFocus = document.activeElement;

    dialog.showModal();

    return () => {
      activeRef.current = false;
      dialog.close();

      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) {
        previousFocus.focus();
      }
    };
  }, []);

  useEffect(() => {
    let active = true;
    const controller = new AbortController();

    chatInvitationApi
      .getCandidates(roomId, page, controller.signal)
      .then((data) => {
        if (!active) return;

        setItems(data.items);
        setHasNext(data.hasNext);
      })
      .catch((err) => {
        if (active) {
          setError(err.message || "팔로잉 목록을 불러오지 못했습니다.");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [roomId, page, revision]);

  function reload() {
    if (busyRef.current || loading) return;

    setLoading(true);
    setError("");
    setRevision((value) => value + 1);
  }

  async function send(person) {
    if (busyRef.current || loading || person.status !== "AVAILABLE") {
      return;
    }

    busyRef.current = true;
    setSendingId(person.userId);
    setError("");
    setCopyMessage("");

    try {
      const result = await chatInvitationApi.send(roomId, person.userId);

      if (!activeRef.current) return;

      setItems((current) =>
        current.map((item) =>
          item.userId === person.userId ? { ...item, status: "INVITED" } : item,
        ),
      );

      setSent({ ...result, nickname: person.nickname });
    } catch (err) {
      if (!activeRef.current) return;

      setError(err.message || "초대 전송에 실패했습니다.");

      // 다른 화면에서 이미 초대했거나 가입한 경우 최신 상태 조회
      if (err.status === 409) {
        setLoading(true);
        setRevision((value) => value + 1);
      }
    } finally {
      busyRef.current = false;

      if (activeRef.current) setSendingId(null);
    }
  }

  async function copyCode() {
    try {
      await navigator.clipboard.writeText(sent.code);

      if (activeRef.current) {
        setCopyMessage("코드를 복사했습니다.");
      }
    } catch {
      if (activeRef.current) {
        setCopyMessage("코드를 직접 선택해 복사해 주세요.");
      }
    }
  }

  function changePage(nextPage) {
    if (busyRef.current || loading) return;

    setLoading(true);
    setError("");
    setPage(nextPage);
  }

  const busy = sendingId !== null;

  return createPortal(
    <dialog
      ref={dialogRef}
      className="following-invite-dialog"
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        event.stopPropagation();
        if (!busyRef.current) onClose();
      }}
    >
      <header className="following-invite-header">
        <div>
          <span>PRIVATE ROOM</span>
          <h2 id={titleId}>친구 초대하기</h2>
          <p>{roomName}</p>
        </div>

        <button
          type="button"
          className="following-invite-close"
          aria-label="초대창 닫기"
          disabled={busy}
          onClick={onClose}
          autoFocus
        >
          ×
        </button>
      </header>

      <div className="following-invite-body">
        {onEnter && (
          <p className="following-invite-help">
            방이 만들어졌어요. 친구를 초대하거나 바로 입장할 수 있어요.
          </p>
        )}

        <div className="following-invite-toolbar">
          <strong>내 팔로잉</strong>
          <button type="button" disabled={busy || loading} onClick={reload}>
            새로고침
          </button>
        </div>

        {error && (
          <p className="following-invite-error" role="alert">
            {error}
          </p>
        )}

        {loading ? (
          <p className="following-invite-help" role="status">
            목록을 불러오는 중…
          </p>
        ) : items.length === 0 ? (
          <p className="following-invite-help">
            {page === 0
              ? "아직 팔로우한 사용자가 없어요. 나중에 프로필에서도 초대할 수 있어요."
              : "이 페이지에 표시할 사용자가 없습니다."}
          </p>
        ) : (
          <ul className="following-invite-list">
            {items.map((person) => (
              <li key={person.userId}>
                <span className="following-invite-avatar" aria-hidden="true">
                  {Array.from(person.nickname || "?")[0]}
                </span>

                <strong>{person.nickname}</strong>

                <button
                  type="button"
                  disabled={busy || person.status !== "AVAILABLE"}
                  onClick={() => send(person)}
                >
                  {sendingId === person.userId
                    ? "전송 중…"
                    : (STATUS_LABELS[person.status] ?? "초대 불가")}
                </button>
              </li>
            ))}
          </ul>
        )}

        <nav className="following-invite-pager" aria-label="팔로잉 목록 페이지">
          <button
            type="button"
            disabled={busy || loading || page === 0}
            onClick={() => changePage(page - 1)}
          >
            이전
          </button>

          <span>{page + 1}페이지</span>

          <button
            type="button"
            disabled={busy || loading || !hasNext}
            onClick={() => changePage(page + 1)}
          >
            다음
          </button>
        </nav>

        {sent && (
          <div className="following-invite-result">
            <strong role="status">
              {sent.nickname}님에게 초대 알림을 보냈어요.
            </strong>

            <label htmlFor={codeId}>최근 발급한 초대코드</label>
            <input
              id={codeId}
              value={sent.code}
              readOnly
              onFocus={(event) => event.target.select()}
            />

            <button type="button" onClick={copyCode}>
              코드 복사
            </button>

            {copyMessage && <p role="status">{copyMessage}</p>}

            <small>
              {new Date(sent.expiresAt).toLocaleString("ko-KR")}까지 유효
              <br />
              초대받은 계정만 사용할 수 있어요. 다른 사람을 초대하기 전에 필요한
              코드를 복사해 주세요.
            </small>
          </div>
        )}
      </div>

      <footer className="following-invite-footer">
        <button
          type="button"
          className="following-invite-primary"
          disabled={busy}
          onClick={onEnter ?? onClose}
        >
          {onEnter ? "채팅방 입장" : "완료"}
        </button>
      </footer>
    </dialog>,
    document.body,
  );
}
