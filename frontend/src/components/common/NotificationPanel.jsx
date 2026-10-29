import { useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { notificationApi } from "../../api/notificationApi";
import ChatReportReviewModal from "../chat/ChatReportReviewModal";
import { useNavigate } from "react-router-dom";
import { chatInvitationApi } from "../../api/chatInvitationApi";
import "../../styles/NotificationInvitation.css";

const notificationTitles = {
  CHAT_REPORT_RECEIVED: "🚨 신고 접수 안내",
  CHAT_REPORT_THRESHOLD: "🚨 신고 검토 요청",
  FOLLOW_RECEIVED: "새로운 팔로워",
  CHAT_ROOM_INVITATION: "✉️ 채팅방 초대",
  POST_COMMENT_RECEIVED: "💬 새로운 댓글",
  POST_LIKE_RECEIVED: "🤍 새로운 좋아요",
};
const invitationStatusLabels = {
  ACCEPTED: "수락한 초대입니다.",
  REJECTED: "거절한 초대입니다.",
  CANCELLED: "취소된 초대입니다.",
  EXPIRED: "만료된 초대입니다.",
  UNAVAILABLE: "사용할 수 없는 초대입니다.",
};

export default function NotificationPanel({ onClose, onRead }) {
  const navigate = useNavigate();
  const dialogRef = useRef(null);
  const controllerRef = useRef(null);
  const activeRef = useRef(false);
  const busyRef = useRef(false);
  const titleId = useId();

  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState(null);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
  const [reviewNotificationId, setReviewNotificationId] = useState(null);
  const mutationVersionRef = useRef(0);

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
      .getList(controller.signal)
      .then((data) => {
        if (!active) return;

        setNotifications(data.notifications.slice(0, 3));
        setError("");
      })
      .catch(() => {
        if (active) {
          setError("알림을 불러오지 못했습니다. 잠시 후 다시 확인해 주세요.");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    let refreshing = false;

    async function refreshNotifications() {
      if (
        !active ||
        refreshing ||
        busyRef.current ||
        document.visibilityState !== "visible"
      ) {
        return;
      }

      refreshing = true;
      const requestVersion = mutationVersionRef.current;

      try {
        const data = await notificationApi.getList(controller.signal);

        if (
          !active ||
          busyRef.current ||
          requestVersion !== mutationVersionRef.current
        ) {
          return;
        }

        setNotifications(data.notifications.slice(0, 3));
        setError("");
      } catch {
        // 자동 조회 실패 시 기존 목록 유지
      } finally {
        refreshing = false;
      }
    }

    const timer = window.setInterval(refreshNotifications, 5000);

    return () => {
      active = false;
      activeRef.current = false;
      controller.abort();
      window.clearInterval(timer);
    };
  }, [revision]);

  async function dismissAll() {
    if (busyRef.current || loading || notifications.length === 0) return;

    const controller = controllerRef.current;
    if (!controller || controller.signal.aborted) return;

    busyRef.current = true;
    mutationVersionRef.current += 1;
    setPending({ id: null, action: "dismissAll" });
    setError("");

    try {
      await notificationApi.dismissAll(controller.signal);

      if (!activeRef.current || controller.signal.aborted) return;

      setNotifications([]);
      onRead();

      // 삭제 이후 도착한 새 알림이 있는지도 다시 조회
      setLoading(true);
      setRevision((current) => current + 1);
    } catch {
      if (activeRef.current && !controller.signal.aborted) {
        setError("전체 삭제에 실패했습니다. 다시 시도해 주세요.");
      }
    } finally {
      busyRef.current = false;

      if (activeRef.current && !controller.signal.aborted) {
        setPending(null);
      }
    }
  }

  async function handleAction(item, action) {
    if (busyRef.current || loading) return;
    if (action === "read" && item.read) return;

    const controller = controllerRef.current;
    if (!controller || controller.signal.aborted) return;

    busyRef.current = true;
    mutationVersionRef.current += 1;
    setPending({ id: item.notificationId, action });
    setError("");

    try {
      if (action === "dismiss") {
        await notificationApi.dismiss(item.notificationId, controller.signal);
      } else {
        await notificationApi.markAsRead(
          item.notificationId,
          controller.signal,
        );
      }

      if (!activeRef.current || controller.signal.aborted) return;

      setNotifications((current) =>
        action === "dismiss"
          ? current.filter(
              (notification) =>
                notification.notificationId !== item.notificationId,
            )
          : current.map((notification) =>
              notification.notificationId === item.notificationId
                ? { ...notification, read: true }
                : notification,
            ),
      );

      // 읽음 처리 또는 삭제 직후 종 아이콘의 숫자도 갱신
      onRead();

      if (action === "dismiss") {
        // 삭제 후 다음 알림을 가져와 최대 3개를 다시 표시
        try {
          const data = await notificationApi.getList(controller.signal);

          if (!activeRef.current || controller.signal.aborted) return;

          setNotifications(data.notifications.slice(0, 3));
        } catch {
          if (activeRef.current && !controller.signal.aborted) {
            setError(
              "삭제는 완료됐지만 다음 알림을 불러오지 못했습니다. 새로고침해 주세요.",
            );
          }
        }
      }
    } catch {
      if (activeRef.current && !controller.signal.aborted) {
        setError(
          action === "dismiss"
            ? "알림 삭제에 실패했습니다. 다시 시도해 주세요."
            : "읽음 처리에 실패했습니다. 다시 시도해 주세요.",
        );
      }
    } finally {
      busyRef.current = false;

      if (activeRef.current && !controller.signal.aborted) {
        setPending(null);
      }
    }
  }

  const busy = loading || pending !== null;
  async function handleInvitation(item, action) {
    if (busyRef.current || loading || !item.invitationId) return;

    const controller = controllerRef.current;
    if (!controller || controller.signal.aborted) return;

    busyRef.current = true;
    mutationVersionRef.current += 1;
    setPending({
      id: item.notificationId,
      action: `invite-${action}`,
    });
    setError("");

    try {
      let result;

      if (action === "accept") {
        result = await chatInvitationApi.accept(
          item.invitationId,
          controller.signal,
        );
      } else {
        await chatInvitationApi.reject(item.invitationId, controller.signal);
      }

      if (!activeRef.current || controller.signal.aborted) return;

      setNotifications((current) =>
        current.map((notification) =>
          notification.notificationId === item.notificationId
            ? {
                ...notification,
                read: true,
                invitationStatus: action === "accept" ? "ACCEPTED" : "REJECTED",
              }
            : notification,
        ),
      );

      onRead();

      if (action === "accept") {
        // 초대 수락 API에서 가입까지 완료되므로 enterRoom 호출 불필요
        onClose();
        navigate(`/chat/${result.roomId}`);
      }
    } catch (err) {
      if (!activeRef.current || controller.signal.aborted) return;

      // 만료되거나 더 이상 접근할 수 없는 초대의 버튼 정리
      if (err.status === 410 || err.status === 404) {
        setNotifications((current) =>
          current.map((notification) =>
            notification.notificationId === item.notificationId
              ? {
                  ...notification,
                  invitationStatus:
                    err.status === 410 ? "EXPIRED" : "UNAVAILABLE",
                }
              : notification,
          ),
        );
      }

      setError(
        err.message ||
          (action === "accept"
            ? "초대 수락에 실패했습니다."
            : "초대 거절에 실패했습니다."),
      );
    } finally {
      busyRef.current = false;

      if (activeRef.current && !controller.signal.aborted) {
        setPending(null);
      }
    }
  }

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
          <p>신고, 팔로우, 채팅방 초대 소식을 확인하세요.</p>
        </div>

        <button
          type="button"
          className="notification-close"
          aria-label="알림창 닫기"
          onClick={onClose}
          autoFocus
        >
          ×
        </button>
      </header>

      <div className="notification-toolbar">
        <span>최신 알림 최대 3개</span>

        <button
          type="button"
          className="notification-action"
          onClick={dismissAll}
          disabled={busy || notifications.length === 0}
          title="내 알림 전체 삭제"
        >
          {pending?.action === "dismissAll" ? "삭제 중…" : "전체 삭제"}
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
          <p className="notification-empty">새로운 알림이 없습니다.</p>
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
                  {item.type === "FOLLOW_RECEIVED" && (
                    <svg
                      viewBox="0 0 24 24"
                      width="16"
                      height="16"
                      fill="#ffffff"
                      aria-hidden="true"
                      style={{ verticalAlign: "-3px", marginRight: "6px" }}
                    >
                      <circle cx="12" cy="7" r="4" />
                      <path d="M4 21v-2a8 8 0 0 1 16 0v2H4Z" />
                    </svg>
                  )}
                  {notificationTitles[item.type] ?? "🔔 알림"}
                </strong>

                <button
                  type="button"
                  className="notification-close"
                  aria-label={`${notificationTitles[item.type] ?? "알림"} 삭제`}
                  title="알림 삭제"
                  disabled={busy}
                  onClick={() => handleAction(item, "dismiss")}
                >
                  {pending?.id === item.notificationId &&
                  pending.action === "dismiss"
                    ? "…"
                    : "×"}
                </button>
              </div>

              <p className="notification-content">{item.content}</p>
              {item.type === "CHAT_ROOM_INVITATION" && (
                <div className="notification-invitation">
                  {item.invitationStatus === "PENDING" ? (
                    <div className="notification-invitation-actions">
                      <button
                        type="button"
                        className="notification-invitation-accept"
                        disabled={busy || !item.invitationId}
                        onClick={() => handleInvitation(item, "accept")}
                      >
                        {pending?.id === item.notificationId &&
                        pending.action === "invite-accept"
                          ? "입장 중…"
                          : "입장하기"}
                      </button>

                      <button
                        type="button"
                        className="notification-invitation-reject"
                        disabled={busy || !item.invitationId}
                        onClick={() => handleInvitation(item, "reject")}
                      >
                        {pending?.id === item.notificationId &&
                        pending.action === "invite-reject"
                          ? "처리 중…"
                          : "거절"}
                      </button>
                    </div>
                  ) : (
                    <>
                      <p
                        className="notification-invitation-status"
                        role="status"
                      >
                        {invitationStatusLabels[item.invitationStatus] ??
                          "초대 상태를 확인할 수 없습니다."}
                      </p>

                      {item.invitationStatus === "ACCEPTED" && (
                        <button
                          type="button"
                          className="notification-invitation-accept"
                          disabled={busy}
                          onClick={() => handleInvitation(item, "accept")}
                        >
                          {pending?.id === item.notificationId &&
                          pending.action === "invite-accept"
                            ? "확인 중…"
                            : "채팅방으로 이동"}
                        </button>
                      )}
                    </>
                  )}
                </div>
              )}
              {item.type === "CHAT_REPORT_THRESHOLD" && (
                <button
                  type="button"
                  className="notification-action"
                  disabled={busy}
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
                    onClick={() => handleAction(item, "read")}
                  >
                    {pending?.id === item.notificationId &&
                    pending.action === "read"
                      ? "처리 중…"
                      : "읽음 처리"}
                  </button>
                )}
              </div>
            </article>
          ))}
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
