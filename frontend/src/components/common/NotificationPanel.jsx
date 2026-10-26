import { useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { notificationApi } from "../../api/notificationApi";
import ChatReportReviewModal from "../chat/ChatReportReviewModal";

export default function NotificationPanel({ onClose, onRead }) {
  const dialogRef = useRef(null);
  const controllerRef = useRef(null);
  const activeRef = useRef(false);
  const busyRef = useRef(false);
  const titleId = useId();

  const [notifications, setNotifications] = useState([]);
  const [nextCursor, setNextCursor] = useState(null);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [readingId, setReadingId] = useState(null);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
  const [reviewNotificationId, setReviewNotificationId] = useState(null);
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
    let active = true;
    const controller = new AbortController();

    activeRef.current = true;
    controllerRef.current = controller;

    notificationApi
      .getList(null, controller.signal)
      .then((data) => {
        if (!active) return;

        setNotifications(data.notifications);
        setNextCursor(data.nextCursor);
        setHasNext(data.hasNext);
      })
      .catch(() => {
        if (active) {
          setError("알림을 불러오지 못했습니다.");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
      activeRef.current = false;
      controller.abort();
    };
  }, [revision]);

  function reload() {
    if (busyRef.current || loading) return;

    setLoading(true);
    setError("");
    setNotifications([]);
    setNextCursor(null);
    setHasNext(false);
    setRevision((current) => current + 1);
  }

  async function loadMore() {
    if (busyRef.current || loading || !hasNext || nextCursor === null) {
      return;
    }

    busyRef.current = true;
    setLoadingMore(true);
    setError("");

    try {
      const data = await notificationApi.getList(
        nextCursor,
        controllerRef.current.signal,
      );

      if (!activeRef.current) return;

      setNotifications((current) => {
        const merged = new Map(
          current.map((item) => [item.notificationId, item]),
        );

        for (const item of data.notifications) {
          if (!merged.has(item.notificationId)) {
            merged.set(item.notificationId, item);
          }
        }

        return [...merged.values()];
      });

      setNextCursor(data.nextCursor);
      setHasNext(data.hasNext);
    } catch {
      if (activeRef.current) {
        setError("이전 알림을 불러오지 못했습니다. 다시 시도해 주세요.");
      }
    } finally {
      busyRef.current = false;
      if (activeRef.current) setLoadingMore(false);
    }
  }

  async function markAsRead(item) {
    if (busyRef.current || loading || item.read) return;

    busyRef.current = true;
    setReadingId(item.notificationId);
    setError("");

    try {
      await notificationApi.markAsRead(
        item.notificationId,
        controllerRef.current.signal,
      );

      if (!activeRef.current) return;

      setNotifications((current) =>
        current.map((notification) =>
          notification.notificationId === item.notificationId
            ? { ...notification, read: true }
            : notification,
        ),
      );

      onRead();
    } catch {
      if (activeRef.current) {
        setError("읽음 처리에 실패했습니다. 다시 시도해 주세요.");
      }
    } finally {
      busyRef.current = false;
      if (activeRef.current) setReadingId(null);
    }
  }

  const busy = loadingMore || readingId !== null;

  return createPortal(
    <dialog
      ref={dialogRef}
      className="notification-dialog"
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
    >
      <header className="notification-header">
        <div>
          <h2 id={titleId}>알림</h2>
          <p>신고 접수와 관리 알림을 확인하세요.</p>
        </div>

        <button
          type="button"
          className="notification-close"
          aria-label="알림 닫기"
          onClick={onClose}
          autoFocus
        >
          ×
        </button>
      </header>

      <div className="notification-toolbar">
        <span>최신 알림순</span>
        <button
          type="button"
          className="notification-action"
          onClick={reload}
          disabled={loading || busy}
        >
          새로고침
        </button>
      </div>

      <div className="notification-body">
        {loading && (
          <p className="notification-empty" role="status">
            알림을 불러오는 중입니다.
          </p>
        )}

        {error && (
          <p className="notification-error" role="alert">
            {error}
          </p>
        )}

        {!loading && !error && notifications.length === 0 && (
          <p className="notification-empty">아직 알림이 없습니다.</p>
        )}

        {!loading &&
          notifications.map((item) => (
            <article
              key={item.notificationId}
              className={`notification-item ${
                item.read ? "is-read" : "is-unread"
              }`}
            >
              <div className="notification-item-heading">
                <strong>
                  {item.type === "CHAT_REPORT_THRESHOLD"
                    ? "신고 검토 요청"
                    : "신고 접수 안내"}
                </strong>

                {!item.read && (
                  <span className="notification-new">새 알림</span>
                )}
              </div>

              <p className="notification-content">{item.content}</p>
              {item.type === "CHAT_REPORT_THRESHOLD" && (
                <button
                  type="button"
                  className="notification-action"
                  disabled={busy || loading}
                  onClick={() => setReviewNotificationId(item.notificationId)}
                >
                  신고 검토
                </button>
              )}
              <div className="notification-item-footer">
                <time dateTime={item.createdAt}>
                  {new Date(item.createdAt).toLocaleString("ko-KR")}
                </time>

                {item.read ? (
                  <span className="notification-read-label">읽음</span>
                ) : (
                  <button
                    type="button"
                    className="notification-action"
                    disabled={busy}
                    onClick={() => markAsRead(item)}
                  >
                    {readingId === item.notificationId
                      ? "처리 중…"
                      : "읽음 처리"}
                  </button>
                )}
              </div>
            </article>
          ))}

        {!loading && hasNext && (
          <button
            type="button"
            className="notification-more"
            onClick={loadMore}
            disabled={busy}
          >
            {loadingMore ? "불러오는 중…" : "이전 알림 더 보기"}
          </button>
        )}
      </div>
      {reviewNotificationId !== null && (
        <ChatReportReviewModal
          key={reviewNotificationId}
          notificationId={reviewNotificationId}
          onClose={() => setReviewNotificationId(null)}
          onProcessed={(notificationId) => {
            setNotifications((current) =>
              current.map((item) =>
                item.notificationId === notificationId
                  ? { ...item, read: true }
                  : item,
              ),
            );

            onRead();
          }}
        />
      )}
    </dialog>,
    document.body,
  );
}
