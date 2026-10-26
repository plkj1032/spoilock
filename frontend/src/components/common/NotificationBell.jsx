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
    const controller = new AbortController();

    async function refreshCount() {
      if (fetching) return;
      fetching = true;

      try {
        const data = await notificationApi.getUnreadCount(controller.signal);

        if (active) {
          setUnreadCount(data.unreadCount);
        }
      } catch {
        if (active) {
          // 조회 실패를 안 읽은 알림 0개로 표시하지 않음
          setUnreadCount(null);
        }
      } finally {
        fetching = false;
      }
    }

    void refreshCount();

    const timer = window.setInterval(refreshCount, 15000);

    return () => {
      active = false;
      controller.abort();
      window.clearInterval(timer);
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
