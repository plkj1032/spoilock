import { useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { chatApi } from "../../api/chatApi";

import "../../styles/ChatOwnershipModal.css";

export default function ChatOwnershipModal({
  roomId,
  currentUserId,
  roomName,
  onClose,
  onLeft,
}) {
  const dialogRef = useRef(null);
  const busyRef = useRef(false);
  const titleId = useId();

  const [members, setMembers] = useState([]);
  const [selectedId, setSelectedId] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    const dialog = dialogRef.current;
    const previousFocus = document.activeElement;
    const previousOverflow = document.body.style.overflow;

    dialog.showModal();
    document.body.style.overflow = "hidden";

    return () => {
      dialog.close();
      document.body.style.overflow = previousOverflow;

      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) {
        previousFocus.focus();
      }
    };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    let timer;

    async function loadMembers() {
      try {
        const data = await chatApi.getMembers(roomId, {
          signal: controller.signal,
        });

        if (!active) return;

        setMembers(data);
        setLoadError("");
      } catch (error) {
        if (active && !controller.signal.aborted) {
          setLoadError(error.message || "참여자를 불러오지 못했습니다.");
        }
      } finally {
        if (active) {
          setLoading(false);
          timer = window.setTimeout(loadMembers, 5000);
        }
      }
    }

    void loadMembers();

    return () => {
      active = false;
      controller.abort();
      window.clearTimeout(timer);
    };
  }, [roomId, revision]);

  const candidates = members
    .filter((member) => String(member.userId) !== String(currentUserId))
    .sort(
      (a, b) =>
        Number(b.online) - Number(a.online) ||
        a.nickname.localeCompare(b.nickname, "ko"),
    );

  const selectedMember = candidates.find(
    (member) => String(member.userId) === selectedId,
  );

  function close() {
    if (!busyRef.current) onClose();
  }

  function reload() {
    if (busyRef.current) return;
    setLoading(true);
    setLoadError("");
    setRevision((value) => value + 1);
  }

  async function submit() {
    if (busyRef.current || loading || loadError) return;
    if (candidates.length > 0 && !selectedMember) return;

    busyRef.current = true;
    setBusy(true);
    setSubmitError("");

    try {
      if (candidates.length === 0) {
        await chatApi.leaveRoom(roomId);
      } else {
        await chatApi.transferOwnershipAndLeave(roomId, selectedMember.userId);
      }

      onLeft();
    } catch (error) {
      setSubmitError(
        error.message || "방장 위임에 실패했습니다. 다시 시도해 주세요.",
      );

      // 선택한 회원이 나갔거나 방장 상태가 바뀌었을 수 있으므로 재조회
      setLoading(true);
      setSelectedId("");
      setRevision((value) => value + 1);
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  }

  return createPortal(
    <dialog
      ref={dialogRef}
      className="chat-owner-dialog"
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        close();
      }}
    >
      <header className="chat-owner-header">
        <div>
          <span>ROOM OWNER</span>
          <h2 id={titleId}>방장 위임 후 나가기</h2>
          <p>{roomName}</p>
        </div>

        <button
          type="button"
          className="chat-owner-close"
          aria-label="방장 위임창 닫기"
          onClick={close}
          disabled={busy}
          autoFocus
        >
          ×
        </button>
      </header>

      <p className="chat-owner-description">
        채팅방을 이어서 관리할 참여자를 선택해 주세요. 접속하지 않은
        참여자에게도 위임할 수 있어요.
      </p>

      <div className="chat-owner-toolbar">
        <span>참여자 {candidates.length}명</span>
        <button type="button" onClick={reload} disabled={busy || loading}>
          새로고침
        </button>
      </div>

      {loading ? (
        <p className="chat-owner-empty" role="status">
          참여자를 확인하고 있어요…
        </p>
      ) : loadError ? (
        <p className="chat-owner-error" role="alert">
          {loadError}
        </p>
      ) : candidates.length === 0 ? (
        <p className="chat-owner-empty">
          다른 참여자가 없습니다.
          <br />
          나가면 기존 자동 삭제 규칙에 따라 방이 정리됩니다.
        </p>
      ) : (
        <div className="chat-owner-list" aria-label="새 방장 선택">
          {candidates.map((member) => {
            const selected = String(member.userId) === selectedId;

            return (
              <label
                key={member.userId}
                className={`chat-owner-member ${
                  member.online ? "" : "is-offline"
                } ${selected ? "is-selected" : ""}`}
              >
                <input
                  type="radio"
                  name={`next-owner-${titleId}`}
                  value={member.userId}
                  checked={selected}
                  disabled={busy}
                  onChange={() => setSelectedId(String(member.userId))}
                />

                <span className="chat-owner-avatar" aria-hidden="true">
                  {Array.from(member.nickname || "?")[0]}
                  {member.online && <i />}
                </span>

                <span className="chat-owner-member-copy">
                  <strong>{member.nickname}</strong>
                  <small>
                    {member.online ? "현재 방에 접속 중" : "현재 접속하지 않음"}
                  </small>
                </span>
              </label>
            );
          })}
        </div>
      )}

      {selectedMember && !loadError && (
        <p className="chat-owner-confirm">
          <strong>{selectedMember.nickname}</strong>님에게 방장을 위임하고
          채팅방에서 나갑니다.
        </p>
      )}

      {submitError && (
        <p className="chat-owner-error" role="alert">
          {submitError}
        </p>
      )}

      <footer className="chat-owner-actions">
        <button type="button" onClick={close} disabled={busy}>
          취소
        </button>

        <button
          type="button"
          className="chat-owner-submit"
          onClick={submit}
          disabled={
            busy ||
            loading ||
            Boolean(loadError) ||
            (candidates.length > 0 && !selectedMember)
          }
        >
          {busy
            ? "처리 중…"
            : candidates.length === 0
              ? "채팅방 나가기"
              : "위임하고 나가기"}
        </button>
      </footer>
    </dialog>,
    document.body,
  );
}
