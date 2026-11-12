import { useEffect, useState } from "react";
import { notificationApi } from "../../api/notificationApi";
import NotificationPanel from "./NotificationPanel";
import "../../styles/Notification.css";

export default function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(null);
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    let active = true;
    let fetching = false;
    let failures = 0;
    let nextAttemptAt = 0;

    const controller = new AbortController();

    async function refreshCount() {
      if (
        !active ||
        fetching ||
        document.visibilityState !== "visible" ||
        Date.now() < nextAttemptAt
      ) {
        return;
      }

      fetching = true;

      try {
        const data = await notificationApi.getUnreadCount(controller.signal);

        if (!active) return;

        setUnreadCount(data.unreadCount);
        failures = 0;
        nextAttemptAt = Date.now() + 5000;
      } catch (error) {
        if (!active || controller.signal.aborted) return;

        // 조회 실패를 '알림 0개'로 표시하지 않음
        setUnreadCount(null);
        failures += 1;

        // 인증 관련 실패는 30초부터, 그 외에는 10초부터 재시도
        const baseDelay =
          error.status === 401 || error.status === 403 ? 30000 : 10000;

        const delay = Math.min(
          baseDelay * 2 ** Math.min(failures - 1, 3),
          120000,
        );

        nextAttemptAt = Date.now() + delay;
      } finally {
        fetching = false;
      }
    }

    function refreshWhenVisible() {
      if (document.visibilityState === "visible") {
        void refreshCount();
      }
    }

    void refreshCount();

    const timer = window.setInterval(refreshCount, 5000);

    document.addEventListener("visibilitychange", refreshWhenVisible);

    return () => {
      active = false;
      controller.abort();
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
    };
  }, [revision]);

  return (
    <>
      <button
        type="button"
        className="notification-bell"
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-label={
          unreadCount === null
            ? "알림 열기"
            : `알림 열기, 안 읽은 알림 ${unreadCount}개`
        }
        onClick={() => {
          setOpen(true);
          setRevision((current) => current + 1);
        }}
      >
        <svg
          viewBox="0 0 24 24"
          width="21"
          height="21"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.7"
          aria-hidden="true"
        >
          <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
          <path d="M10 21h4" />
        </svg>

        {unreadCount > 0 && (
          <span className="notification-badge" aria-hidden="true">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <NotificationPanel
          onClose={() => setOpen(false)}
          onRead={() => setRevision((current) => current + 1)}
        />
      )}
    </>
  );
}
