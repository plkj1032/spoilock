import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { chatInviteLinkApi, inviteLinkError } from "../api/chatInviteLinkApi";

import "../styles/ChatInvitePage.css";

export default function ChatInvitePage({ user, pending, onLogin }) {
  const { code = "" } = useParams();
  const navigate = useNavigate();

  const userId = user?.id;
  const lifetimeRef = useRef(null);
  const submittingRef = useRef(false);

  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const [revision, setRevision] = useState(0);

  const requestKey = `${userId ?? "guest"}:${code}:${revision}`;

  // 계정이나 링크가 바뀌면 이전 요청의 결과를 사용하지 않음
  useEffect(() => {
    const lifetime = { active: true };

    lifetimeRef.current = lifetime;

    return () => {
      lifetime.active = false;
    };
  }, [userId, code]);

  // 로그인한 사용자에게만 초대 내용 조회
  useEffect(() => {
    if (pending || userId == null) return;

    let active = true;
    const controller = new AbortController();

    chatInviteLinkApi
      .preview(code, controller.signal)
      .then((data) => {
        if (!active) return;

        setResult({
          key: requestKey,
          data,
          error: "",
        });
      })
      .catch((error) => {
        if (!active || error.name === "AbortError") return;

        setResult({
          key: requestKey,
          data: null,
          error: inviteLinkError(error),
        });
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [pending, userId, code, requestKey]);

  function loginToContinue() {
    // 카카오 로그인으로 현재 페이지를 떠나도 돌아올 위치 저장
    try {
      sessionStorage.setItem(
        "spoilock.inviteReturn",
        JSON.stringify({
          path: `/chat/invite/${encodeURIComponent(code)}`,
          savedAt: Date.now(),
        }),
      );
    } catch {
      // 저장이 불가능해도 현재 페이지에서 이메일 로그인은 가능
    }

    onLogin();
  }

  async function acceptInvitation() {
    if (submittingRef.current || userId == null) return;

    const lifetime = lifetimeRef.current;

    if (!lifetime?.active) return;

    submittingRef.current = true;
    setBusy(true);

    try {
      const response = await chatInviteLinkApi.accept(code);

      if (!lifetime.active) return;

      // 수락 API에서 이미 가입 처리하므로 enterRoom을 다시 호출하지 않음
      navigate(`/chat/${response.roomId}`, {
        replace: true,
      });
    } catch (error) {
      if (!lifetime.active) return;

      setResult({
        key: requestKey,
        data: null,
        error: inviteLinkError(error),
      });
    } finally {
      submittingRef.current = false;

      if (lifetime.active) {
        setBusy(false);
      }
    }
  }

  const current = result?.key === requestKey ? result : null;
  const invitation = current?.data;

  return (
    <main className="container main-content chat-invite-page">
      <section className="chat-invite-card" aria-labelledby="chat-invite-title">
        <div className="chat-invite-symbol" aria-hidden="true">
          <svg
            viewBox="0 0 24 24"
            width="28"
            height="28"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.7"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <rect x="3" y="5" width="18" height="14" rx="3" />
            <path d="m4 7 8 6 8-6" />
          </svg>
        </div>

        <span className="chat-invite-overline">PRIVATE INVITATION</span>

        <h1 id="chat-invite-title">비밀 채팅방 초대</h1>

        {pending ? (
          <p className="chat-invite-description" role="status">
            로그인 상태를 확인하고 있어요.
          </p>
        ) : !user ? (
          <>
            <p className="chat-invite-description">
              로그인하면 초대받은 방을 확인하고
              <br />
              대화에 참여할 수 있어요.
            </p>

            <button
              type="button"
              className="chat-invite-primary"
              onClick={loginToContinue}
            >
              로그인하고 초대 확인
            </button>
          </>
        ) : !current ? (
          <p className="chat-invite-description" role="status">
            초대 정보를 확인하고 있어요.
          </p>
        ) : current.error ? (
          <>
            <div className="chat-invite-error" role="alert">
              {current.error}
            </div>

            <button
              type="button"
              className="chat-invite-secondary"
              onClick={() => setRevision((value) => value + 1)}
            >
              다시 확인
            </button>
          </>
        ) : (
          <>
            <p className="chat-invite-description">
              <strong>{invitation.inviterNickname}</strong>님이 초대했어요.
            </p>

            <div className="chat-invite-room">
              <span className="chat-invite-badge">비밀 채팅방</span>

              <h2>{invitation.roomName}</h2>

              <p>{invitation.dramaTitle}</p>
            </div>

            <dl className="chat-invite-details">
              <div>
                <dt>초대 만료</dt>
                <dd>
                  {new Date(invitation.expiresAt).toLocaleString("ko-KR")}
                </dd>
              </div>

              <div>
                <dt>남은 초대 인원</dt>
                <dd>{invitation.remainingUses}명</dd>
              </div>
            </dl>

            {invitation.joined && (
              <p className="chat-invite-joined">이미 참여 중인 채팅방이에요.</p>
            )}

            <button
              type="button"
              className="chat-invite-primary"
              disabled={busy}
              onClick={acceptInvitation}
            >
              {busy
                ? "입장 중…"
                : invitation.joined
                  ? "채팅방으로 이동"
                  : "초대 수락하고 입장"}
            </button>

            {!invitation.joined && (
              <p className="chat-invite-footnote">
                수락하면 이 채팅방의 참여자로 등록돼요.
              </p>
            )}
          </>
        )}

        <Link to="/chat" className="chat-invite-back">
          채팅방 목록으로
        </Link>
      </section>
    </main>
  );
}
