import { useEffect, useId, useRef, useState } from "react";

import {
  chatInviteLinkApi,
  inviteLinkError,
} from "../../api/chatInviteLinkApi";

import "../../styles/ChatInviteLink.css";

const STATUS_LABELS = {
  ACTIVE: "사용 가능",
  FULL: "초대 인원 마감",
  REVOKED: "취소됨",
  EXPIRED: "만료됨",
};

export default function ChatInviteLinkPanel({
  roomId,
  disabled = false,
  onBusyChange,
}) {
  const limitId = useId();
  const urlId = useId();

  const activeRef = useRef(false);
  const busyRef = useRef(false);

  const [link, setLink] = useState(null);
  const [inviteUrl, setInviteUrl] = useState("");
  const [maxUses, setMaxUses] = useState("10");

  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [revision, setRevision] = useState(0);

  const [loadError, setLoadError] = useState("");
  const [actionError, setActionError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    activeRef.current = true;

    return () => {
      activeRef.current = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    const controller = new AbortController();

    chatInviteLinkApi
      .current(roomId, controller.signal)
      .then((items) => {
        if (!active) return;

        setLink(items[0] ?? null);
        setLoadError("");
      })
      .catch((error) => {
        if (!active || error.name === "AbortError") return;

        setLoadError(inviteLinkError(error));
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [roomId, revision]);

  function beginAction() {
    busyRef.current = true;
    setBusy(true);
    onBusyChange?.(true);

    setActionError("");
    setMessage("");
  }

  function finishAction() {
    busyRef.current = false;

    if (activeRef.current) {
      setBusy(false);
      onBusyChange?.(false);
    }
  }

  function reload() {
    if (busyRef.current || disabled || loading) return;

    setLoading(true);
    setLoadError("");
    setActionError("");
    setMessage("");

    // 다른 창에서 재발급했을 수 있으므로 예전 주소는 숨김
    setInviteUrl("");
    setRevision((value) => value + 1);
  }

  async function issueLink() {
    if (busyRef.current || disabled || loading || loadError) return;

    const count = Number(maxUses);

    if (!Number.isInteger(count) || count < 1 || count > 100) {
      setActionError("초대 가능 인원은 1~100명으로 입력해 주세요.");
      return;
    }

    let publicOrigin;

    try {
      const configuredUrl =
        import.meta.env.VITE_PUBLIC_APP_URL || window.location.origin;

      const parsed = new URL(configuredUrl);

      if (!["http:", "https:"].includes(parsed.protocol)) {
        throw new Error("잘못된 주소");
      }

      publicOrigin = parsed.origin;
    } catch {
      setActionError("공유 링크에 사용할 서비스 주소 설정을 확인해 주세요.");
      return;
    }

    if (
      link?.status === "ACTIVE" &&
      !window.confirm(
        "새 링크를 발급하면 기존 공유 링크는 사용할 수 없습니다.\n재발급할까요?",
      )
    ) {
      return;
    }

    beginAction();

    try {
      const result = await chatInviteLinkApi.issue(roomId, count);

      if (!activeRef.current) return;

      const url = new URL(
        `/chat/invite/${encodeURIComponent(result.code)}`,
        publicOrigin,
      );

      setLink(result.link);
      setInviteUrl(url.href);
      setMessage("초대 링크를 발급했어요. 복사해서 공유해 주세요.");
    } catch (error) {
      if (!activeRef.current) return;

      // 응답을 받지 못했어도 서버에서 재발급됐을 수 있음
      setInviteUrl("");
      setActionError(inviteLinkError(error));
    } finally {
      finishAction();
    }
  }

  async function revokeLink() {
    if (busyRef.current || disabled || loading || !link) return;

    if (
      !window.confirm(
        "이 공유 링크를 취소할까요?\n이미 입장한 참여자는 그대로 유지됩니다.",
      )
    ) {
      return;
    }

    beginAction();

    try {
      await chatInviteLinkApi.revoke(roomId, link.id);

      if (!activeRef.current) return;

      setLink((current) => ({
        ...current,
        status: "REVOKED",
      }));

      setInviteUrl("");
      setMessage("공유 링크를 취소했어요.");
    } catch (error) {
      if (!activeRef.current) return;

      setInviteUrl("");
      setActionError(inviteLinkError(error));
    } finally {
      finishAction();
    }
  }

  async function copyLink() {
    if (!inviteUrl) return;

    try {
      await navigator.clipboard.writeText(inviteUrl);

      if (activeRef.current) {
        setMessage("초대 링크를 복사했어요.");
      }
    } catch {
      if (activeRef.current) {
        setMessage("주소를 직접 선택해서 복사해 주세요.");
      }
    }
  }

  const blocked = disabled || busy || loading;

  return (
    <section className="invite-link-panel" aria-busy={busy || loading}>
      <div className="invite-link-heading">
        <div>
          <span className="invite-link-overline">INVITE LINK</span>
          <h3>링크로 초대하기</h3>
        </div>

        <span className="invite-link-expiry">24시간 유효</span>
      </div>

      <p className="invite-link-description">
        링크를 받은 사용자가 로그인 후 입장할 수 있어요.
      </p>

      {loading ? (
        <p className="invite-link-description" role="status">
          공유 링크 확인 중…
        </p>
      ) : loadError ? (
        <div>
          <p className="invite-link-error" role="alert">
            {loadError}
          </p>

          <button
            type="button"
            className="invite-link-secondary"
            disabled={disabled || busy}
            onClick={reload}
          >
            다시 확인
          </button>
        </div>
      ) : (
        <>
          {link && (
            <div className="invite-link-status">
              <div>
                <strong>{STATUS_LABELS[link.status] ?? link.status}</strong>
                <span>
                  {link.usedCount} / {link.maxUses}명 사용
                </span>
              </div>

              <small>
                {new Date(link.expiresAt).toLocaleString("ko-KR")}까지
              </small>
            </div>
          )}

          <div className="invite-link-limit-row">
            <label htmlFor={limitId}>초대 가능 인원</label>

            <div>
              <input
                id={limitId}
                type="number"
                min="1"
                max="100"
                step="1"
                value={maxUses}
                disabled={blocked}
                onChange={(event) => {
                  setMaxUses(event.target.value);
                  setActionError("");
                }}
              />
              <span>명</span>
            </div>
          </div>

          <div className="invite-link-actions">
            <button
              type="button"
              className="invite-link-primary"
              disabled={blocked}
              onClick={issueLink}
            >
              {busy ? "처리 중…" : link ? "새 링크 발급" : "초대 링크 발급"}
            </button>

            {link && ["ACTIVE", "FULL"].includes(link.status) && (
              <button
                type="button"
                className="invite-link-secondary"
                disabled={blocked}
                onClick={revokeLink}
              >
                링크 취소
              </button>
            )}

            <button
              type="button"
              className="invite-link-secondary"
              disabled={blocked}
              onClick={reload}
            >
              상태 확인
            </button>
          </div>

          {inviteUrl && link?.status === "ACTIVE" ? (
            <div className="invite-link-copy">
              <label htmlFor={urlId}>공유할 초대 링크</label>

              <div>
                <input
                  id={urlId}
                  value={inviteUrl}
                  readOnly
                  onFocus={(event) => event.target.select()}
                />

                <button
                  type="button"
                  className="invite-link-primary"
                  disabled={blocked}
                  onClick={copyLink}
                >
                  복사
                </button>
              </div>
            </div>
          ) : (
            <p className="invite-link-help">
              링크 주소는 발급 직후에만 표시돼요. 복사하지 않고 창을 닫았다면
              새로 발급해 주세요.
            </p>
          )}
        </>
      )}

      {actionError && (
        <p className="invite-link-error" role="alert">
          {actionError}
        </p>
      )}

      {message && (
        <p className="invite-link-message" role="status">
          {message}
        </p>
      )}
    </section>
  );
}
