import { useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { notificationApi } from "../../api/notificationApi";
import "../../styles/ChatReportReviewModal.css";

export default function ChatReportReviewModal({
  notificationId,
  onClose,
  onProcessed,
}) {
  const dialogRef = useRef(null);
  const controllerRef = useRef(null);
  const submittingRef = useRef(false);
  const titleId = useId();

  const [review, setReview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState("");
  const [decision, setDecision] = useState(null);
  const [mustReload, setMustReload] = useState(false);
  const [revision, setRevision] = useState(0);

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

  useEffect(() => {
    const controller = new AbortController();
    controllerRef.current = controller;

    notificationApi
      .getReportReview(notificationId, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          setReview(data);
        }
      })
      .catch((err) => {
        if (controller.signal.aborted) return;

        setError(
          err.status === 403
            ? "이 신고를 검토할 권한이 없습니다."
            : err.status === 404
              ? "신고 검토 알림을 찾을 수 없습니다."
              : "신고 내용을 불러오지 못했습니다.",
        );
        setMustReload(true);
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [notificationId, revision]);

  function close() {
    if (!submittingRef.current) onClose();
  }

  function reload() {
    if (submittingRef.current || loading) return;

    setReview(null);
    setLoading(true);
    setError("");
    setDecision(null);
    setMustReload(false);
    setRevision((current) => current + 1);
  }

  async function submit() {
    if (
      submittingRef.current ||
      !decision ||
      !review ||
      review.processed ||
      mustReload ||
      result
    ) {
      return;
    }

    const signal = controllerRef.current.signal;
    const selectedDecision = decision;

    submittingRef.current = true;
    setSubmitting(true);
    setError("");

    try {
      await notificationApi.decideReport(
        notificationId,
        selectedDecision,
        review.reports.map((report) => report.reportId),
        signal,
      );

      if (signal.aborted) return;

      setResult(
        selectedDecision === "KICK"
          ? "회원을 강퇴하고 신고를 승인했습니다."
          : "신고를 반려했습니다. 회원은 강퇴되지 않았습니다.",
      );
      setDecision(null);
      onProcessed(notificationId);
    } catch (err) {
      if (signal.aborted) return;

      const messages = {
        400: "처리할 수 없는 요청입니다.",
        401: "로그인 상태를 확인해 주세요.",
        403: "현재 이 신고를 처리할 권한이 없습니다.",
        404: "신고 대상을 찾을 수 없거나 해당 회원이 방을 나갔습니다.",
        409: "신고 목록 또는 회원 상태가 변경되었거나 이미 처리되었습니다.",
      };

      setError(
        messages[err.status] ||
          "처리 결과를 확인하지 못했습니다. 다시 조회해 주세요.",
      );

      // 실패·응답 유실 시 이전 화면으로 재처리하지 않도록 재조회 요구
      setMustReload(true);
      setDecision(null);
    } finally {
      submittingRef.current = false;

      if (!signal.aborted) {
        setSubmitting(false);
      }
    }
  }

  const canDecide =
    review &&
    !review.processed &&
    review.reports.length > 0 &&
    !loading &&
    !mustReload &&
    !result;

  return createPortal(
    <dialog
      ref={dialogRef}
      className="report-review-dialog"
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        // 뒤에 있는 알림 목록까지 함께 닫히지 않도록 처리
        event.stopPropagation();
        close();
      }}
    >
      <header className="report-review-header">
        <h2 id={titleId}>신고 검토</h2>

        <button
          type="button"
          className="report-review-close"
          aria-label="신고 검토 닫기"
          onClick={close}
          disabled={submitting}
          autoFocus
        >
          ×
        </button>
      </header>

      <div className="report-review-body">
        {loading && <p role="status">신고 내용을 불러오는 중입니다.</p>}

        {error && (
          <div className="report-review-error" role="alert">
            <p>{error}</p>
            <button
              type="button"
              className="report-review-secondary"
              onClick={reload}
              disabled={loading || submitting}
            >
              다시 조회
            </button>
          </div>
        )}

        {result ? (
          <div className="report-review-result">
            <p role="status">{result}</p>
            <button
              type="button"
              className="report-review-secondary"
              onClick={close}
            >
              확인
            </button>
          </div>
        ) : (
          !loading &&
          review && (
            <>
              <div className="report-review-summary">
                <p>{review.roomName}</p>
                <h3>{review.targetNickname}님에 대한 신고</h3>
                <span>
                  {review.processed
                    ? "이미 처리된 신고입니다."
                    : `미처리 신고 ${review.reports.length}건`}
                </span>
              </div>

              {!review.processed && (
                <ol className="report-review-list">
                  {review.reports.map((report) => (
                    <li key={report.reportId}>
                      <strong>{report.reasonLabel}</strong>

                      <p>신고 사유: {report.detail || "상세 설명 없음"}</p>

                      <details>
                        <summary>
                          신고된 채팅 원문 보기 · 스포일러 포함 가능
                        </summary>

                        <p>
                          <strong>{review.targetNickname}</strong>
                          {" · 메시지 #"}
                          {report.messageId}
                        </p>

                        <p>
                          {report.messageContent ??
                            "메시지 내용을 확인할 수 없습니다."}
                        </p>

                        {report.messageCreatedAt && (
                          <p>
                            작성 시각:{" "}
                            <time dateTime={report.messageCreatedAt}>
                              {new Date(report.messageCreatedAt).toLocaleString(
                                "ko-KR",
                              )}
                            </time>
                          </p>
                        )}
                      </details>

                      <p>
                        신고 시각:{" "}
                        <time dateTime={report.createdAt}>
                          {new Date(report.createdAt).toLocaleString("ko-KR")}
                        </time>
                      </p>
                    </li>
                  ))}
                </ol>
              )}

              {canDecide &&
                (decision ? (
                  <div className="report-review-confirm">
                    <p>
                      {decision === "KICK"
                        ? `${review.targetNickname}님을 강퇴하고, 위 신고 ${review.reports.length}건을 승인할까요?`
                        : `위 신고 ${review.reports.length}건을 반려할까요? 회원은 강퇴되지 않습니다.`}
                    </p>

                    <div className="report-review-actions">
                      <button
                        type="button"
                        className="report-review-secondary"
                        disabled={submitting}
                        onClick={() => setDecision(null)}
                      >
                        돌아가기
                      </button>
                      <button
                        type="button"
                        className={
                          decision === "KICK"
                            ? "report-review-danger"
                            : "report-review-primary"
                        }
                        disabled={submitting}
                        onClick={submit}
                      >
                        {submitting ? "처리 중…" : "처리 확정"}
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="report-review-actions">
                    <button
                      type="button"
                      className="report-review-secondary"
                      onClick={() => setDecision("REJECT")}
                    >
                      신고 반려
                    </button>
                    <button
                      type="button"
                      className="report-review-danger"
                      onClick={() => setDecision("KICK")}
                    >
                      강퇴하고 승인
                    </button>
                  </div>
                ))}
            </>
          )
        )}
      </div>
    </dialog>,
    document.body,
  );
}
