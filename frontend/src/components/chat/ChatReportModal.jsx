import { useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { chatApi } from "../../api/chatApi";
import "../../styles/ChatReportModal.css";

const REASONS = [
  { value: "SPOILER", label: "스포일러" },
  { value: "SPAM", label: "광고 및 도배" },
  { value: "ABUSE", label: "욕설 및 비방" },
  { value: "OTHER", label: "기타" },
];

export default function ChatReportModal({ roomId, message, onClose }) {
  const dialogRef = useRef(null);
  const activeRef = useRef(false);
  const submittingRef = useRef(false);

  const titleId = useId();
  const reasonId = useId();
  const detailId = useId();

  const [reason, setReason] = useState("");
  const [detail, setDetail] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState("");

  useEffect(() => {
    const dialog = dialogRef.current;
    const previousFocus = document.activeElement;

    activeRef.current = true;
    dialog.showModal();

    return () => {
      activeRef.current = false;
      dialog.close();

      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) {
        previousFocus.focus();
      }
    };
  }, []);

  function close() {
    if (!submittingRef.current) {
      onClose();
    }
  }

  async function submit(event) {
    event.preventDefault();

    if (submittingRef.current || result) return;

    if (!reason) {
      setError("신고 사유를 선택해 주세요.");
      return;
    }

    if (detail.length > 500) {
      setError("상세 설명은 500자 이하로 입력해 주세요.");
      return;
    }

    submittingRef.current = true;
    setSubmitting(true);
    setError("");

    try {
      await chatApi.reportMessage(
        Number(roomId),
        message.messageId,
        reason,
        detail,
      );

      if (activeRef.current) {
        setResult("신고가 접수되었습니다.");
      }
    } catch (err) {
      if (!activeRef.current) return;

      if (err.status === 409) {
        setResult("이미 신고한 메시지입니다.");
      } else {
        setError(err.message || "신고 접수에 실패했습니다.");
      }
    } finally {
      submittingRef.current = false;

      if (activeRef.current) {
        setSubmitting(false);
      }
    }
  }

  return createPortal(
    <dialog
      ref={dialogRef}
      className="chat-report-dialog"
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        close();
      }}
    >
      <header className="chat-report-header">
        <div>
          <h2 id={titleId}>메시지 신고</h2>
          <p>{message.nickname}님의 메시지를 신고합니다.</p>
        </div>

        <button
          type="button"
          className="chat-report-close"
          aria-label="신고 창 닫기"
          onClick={close}
          disabled={submitting}
          autoFocus
        >
          ×
        </button>
      </header>

      {result ? (
        <div className="chat-report-result">
          <p role="status">{result}</p>
          <button type="button" className="chat-report-primary" onClick={close}>
            확인
          </button>
        </div>
      ) : (
        <form className="chat-report-form" onSubmit={submit}>
          <label htmlFor={reasonId}>신고 사유</label>
          <select
            id={reasonId}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            disabled={submitting}
            required
          >
            <option value="">사유를 선택해 주세요</option>
            {REASONS.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>

          <div className="chat-report-detail-label">
            <label htmlFor={detailId}>상세 설명 · 선택</label>
            <span>{detail.length}/500</span>
          </div>

          <textarea
            id={detailId}
            value={detail}
            onChange={(event) => setDetail(event.target.value)}
            placeholder="관리자가 확인할 내용을 입력해 주세요."
            maxLength={500}
            rows={4}
            disabled={submitting}
          />

          <p className="chat-report-help">
            신고자 이름은 상대방에게 공개되지 않습니다. 선택한 신고 사유는
            상대방에게 전달되며, 상세 설명은 관리자 확인용으로 사용됩니다.
          </p>

          {error && (
            <p className="chat-report-error" role="alert">
              {error}
            </p>
          )}

          <footer className="chat-report-actions">
            <button
              type="button"
              className="chat-report-secondary"
              onClick={close}
              disabled={submitting}
            >
              취소
            </button>

            <button
              type="submit"
              className="chat-report-primary"
              disabled={submitting || !reason}
            >
              {submitting ? "접수 중…" : "신고 접수"}
            </button>
          </footer>
        </form>
      )}
    </dialog>,
    document.body,
  );
}
