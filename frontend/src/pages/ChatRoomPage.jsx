import { useEffect, useRef, useState, useLayoutEffect } from "react";
import {
  Link,
  useNavigate,
  useParams,
  useSearchParams,
} from "react-router-dom";
import { Client } from "@stomp/stompjs";
import { chatApi } from "../api/chatApi";
import ChatBanPanel from "../components/chat/ChatBanPanel";
import { apiRequest, authApi } from "../api/authApi";
import Button from "../components/common/Button";
import "../styles/ChatRoomPage.css";
import "../styles/ChatScrollControls.css";
import useChatPresence from "../hooks/useChatPresence";
import "../styles/ChatPresence.css";
import ChatMemberPanel from "../components/chat/ChatMemberPanel";

// 서버가 원문 대신 보내는 차단 이유별 안내 (사건 내용은 절대 표시하지 않음)
const BLOCK_MESSAGES = {
  ANALYZING: "⏳ 스포일러 여부를 확인하고 있어요…",
  NOT_WATCHED: "🔒 아직 시청하지 않은 회차의 내용이 포함된 메시지예요.",
  REVIEW: "🔒 스포일러 여부를 확인할 수 없어 가려진 메시지예요.",
  FAILED: "⏳ 확인이 지연되고 있어요. 잠시 후 다시 표시돼요.",
  HIDDEN: "🚫 관리자가 숨긴 메시지예요.",
  DEFAULT: "🔒 아직 공개되지 않은 메시지입니다.",
};

// 같은 메시지는 하나만 유지하고, ID 순으로 정렬
function mergeMessages(current, incoming) {
  const merged = new Map(
    current.map((message) => [message.messageId, message]),
  );

  for (const message of incoming) {
    merged.set(message.messageId, message);
  }

  return [...merged.values()].sort((a, b) => a.messageId - b.messageId);
}

export default function ChatRoomPage({ user }) {
  const { roomId } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  // 개발 환경에서만 두 번째 서버 연결 허용
  const useSecondServer =
    import.meta.env.DEV && searchParams.get("server") === "8081";

  const clientRef = useRef(null);
  const messageListRef = useRef(null);
  const followBottomRef = useRef(true);
  const initialScrollDoneRef = useRef(false);

  const [room, setRoom] = useState(null);
  const [messages, setMessages] = useState([]);
  const [content, setContent] = useState("");
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [leaving, setLeaving] = useState(false);
  const [nextCursor, setNextCursor] = useState(null);
  const [hasNext, setHasNext] = useState(false);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [kickingUserId, setKickingUserId] = useState(null);
  const [showMemberPanel, setShowMemberPanel] = useState(false);
  const [showBanPanel, setShowBanPanel] = useState(false);
  const [connectionAttempt, setConnectionAttempt] = useState(0);
  const kickingRef = useRef(false);
  const [bans, setBans] = useState([]);
  const [bansLoading, setBansLoading] = useState(true);
  const [bansError, setBansError] = useState("");
  const [banRevision, setBanRevision] = useState(0);
  const historyAnchorRef = useRef(null);
  const [isAwayFromBottom, setIsAwayFromBottom] = useState(false);
  const [newMessageCount, setNewMessageCount] = useState(0);
  const seenMessageIdsRef = useRef(new Set());
  const {
    members,
    membersLoading,
    membersError,
    startPresence,
    stopPresence,
    refreshMembers,
  } = useChatPresence(roomId);

  const onlineUserIds = new Set(
    connected
      ? members
          .filter((member) => member.online)
          .map((member) => String(member.userId))
      : [],
  );
  function goToBottom() {
    const list = messageListRef.current;
    if (!list) return;

    historyAnchorRef.current = null;
    followBottomRef.current = true;
    list.scrollTop = list.scrollHeight;

    setIsAwayFromBottom(false);
    setNewMessageCount(0);
  }

  function handleMessageScroll(event) {
    const list = event.currentTarget;
    const distance = list.scrollHeight - list.scrollTop - list.clientHeight;

    const away = distance > 80;

    followBottomRef.current = !away;
    setIsAwayFromBottom(away);

    if (!away) {
      setNewMessageCount(0);
    }
  }
  const bannedUserIds = new Set(bans.map((ban) => String(ban.userId)));

  const canKick = Boolean(
    room &&
    (room.roomType === "DEFAULT"
      ? user.role === "ADMIN"
      : ["CUSTOM", "PRIVATE"].includes(room.roomType) &&
        String(room.createdById) === String(user.id)),
  );
  useEffect(() => {
    if (!canKick) return;

    let active = true;

    chatApi
      .getBans(Number(roomId))
      .then((data) => {
        if (!active) return;

        setBans(data);
        setBansError("");
      })
      .catch((err) => {
        if (!active) return;

        setBansError(
          err.status === 403
            ? "강퇴 목록을 조회할 권한이 없습니다."
            : "강퇴 목록을 불러오지 못했습니다.",
        );
      })
      .finally(() => {
        if (active) setBansLoading(false);
      });

    return () => {
      active = false;
    };
  }, [roomId, canKick, banRevision]);

  useLayoutEffect(() => {
    const list = messageListRef.current;
    if (!list || loading) return;

    const anchor = historyAnchorRef.current;

    if (anchor) {
      const element = Array.from(
        list.querySelectorAll("[data-message-id]"),
      ).find((item) => item.dataset.messageId === anchor.messageId);

      if (element) {
        const currentOffset =
          element.getBoundingClientRect().top -
          list.getBoundingClientRect().top;

        list.scrollTop += currentOffset - anchor.offset;
      }

      historyAnchorRef.current = null;
      return;
    }

    if (!initialScrollDoneRef.current || followBottomRef.current) {
      list.scrollTop = list.scrollHeight;
      initialScrollDoneRef.current = true;
    }
  }, [messages, loading]);

  const olderLoadingRef = useRef(false);
  useEffect(() => {
    let active = true;

    chatApi
      .getRoom(Number(roomId))
      .then((data) => {
        if (active) {
          setRoom(data);
        }
      })
      .catch((error) => {
        if (active) {
          setError(error.message);
        }
      });

    return () => {
      active = false;
    };
  }, [roomId]);

  useEffect(() => {
    let active = true;
    const controller = new AbortController();

    async function getJson(path) {
      try {
        return await apiRequest(path, {
          retryOn401: true,
          cache: "no-store",
          signal: controller.signal,
        });
      } catch (err) {
        if (err.status === 403) {
          throw new Error("이 채팅방에 접근할 권한이 없습니다.", {
            cause: err,
          });
        }

        throw err;
      }
    }

    function showError(err) {
      if (active && !controller.signal.aborted) {
        setError(err.message);
      }
    }

    async function loadHistory() {
      try {
        const data = await getJson(`/api/chat/rooms/${roomId}/messages`);

        if (active) {
          // 이력 조회 중 먼저 도착한 실시간 메시지도 보존
          setMessages((current) => mergeMessages(data.messages, current));
          setNextCursor(data.nextCursor);
          setHasNext(data.hasNext);
          data.messages.forEach((message) => {
            seenMessageIdsRef.current.add(String(message.messageId));
          });
        }
      } catch (err) {
        showError(err);
      } finally {
        if (active) setLoading(false);
      }
    }

    async function receiveMessage(frame) {
      try {
        const event = JSON.parse(frame.body);

        if (String(event.roomId) !== roomId) return;
        if (!active) return;
        if (event.eventType === "PRESENCE_CHANGED") {
          // 다음 단계에서 회원 목록을 다시 조회하도록 연결
          refreshMembers();
          return;
        }
        if (event.eventType == "KICKED") {
          setConnected(false);
          setMessages([]);
          setContent("");

          window.alert("이 채팅방에서 강퇴되었습니다.");
          navigate("/chat", { replace: true });
          return;
        }

        const message = await getJson(
          `/api/chat/rooms/${roomId}/messages/${event.messageId}`,
        );
        if (active && message.messageType !== "CHAT") {
          // 가입·탈퇴·강퇴 알림을 받으면 회원 목록도 갱신
          refreshMembers();
        }

        if (active) {
          const messageId = String(message.messageId);
          const isNew = !seenMessageIdsRef.current.has(messageId);

          seenMessageIdsRef.current.add(messageId);

          if (
            isNew &&
            initialScrollDoneRef.current &&
            !followBottomRef.current &&
            message.messageType === "CHAT" &&
            String(message.userId) !== String(user.id)
          ) {
            setNewMessageCount((current) => current + 1);
          }

          setMessages((current) => mergeMessages(current, [message]));
        }
      } catch (err) {
        showError(err);
      }
    }

    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";

    // Vite의 /ws 프록시를 통해 Spring에 연결
    const client = new Client({
      brokerURL: useSecondServer
        ? import.meta.env.VITE_WS_SECONDARY_URL
        : `${protocol}//${window.location.host}/ws`,
      reconnectDelay: 0,
      connectionTimeout: 5000,

      beforeConnect: async () => {
        if (!active) return;

        setConnected(false);
        setLoading(true);
        setError("");
        setMessages([]);
        setNextCursor(null);
        setHasNext(false);
        setLoadingOlder(false);
        initialScrollDoneRef.current = false;
        historyAnchorRef.current = null;
        followBottomRef.current = true;
        seenMessageIdsRef.current.clear();
        setIsAwayFromBottom(false);
        setNewMessageCount(0);
        try {
          const me = await authApi.restore();

          if (!active) {
            void client.deactivate();
            return;
          }

          if (!me || String(me.id) !== String(user.id)) {
            setConnected(false);
            setLoading(false);
            setError(
              "로그인 상태가 변경되었습니다. 페이지를 새로고침해 주세요.",
            );

            void client.deactivate();
            return;
          }
        } catch {
          if (active) {
            setConnected(false);
            setLoading(false);
            setError("로그인 상태를 확인하지 못했습니다. 다시 시도해 주세요.");
          }

          void client.deactivate();
        }
      },
      onConnect: () => {
        if (!active) return;

        client.subscribe("/user/queue/chat", receiveMessage);

        setConnected(true);
        setError("");

        startPresence(client);
        loadHistory();
      },

      onStompError: () => {
        if (!active) return;
        stopPresence();
        setConnected(false);
        setLoading(false);
        setError(
          "서버가 요청을 거절했습니다. 다시 연결 후에도 반복되면 방 이용 권한을 확인해 주세요.",
        );

        void client.deactivate();
      },

      onWebSocketError: () => {
        if (active) {
          setError("채팅 서버에 연결하지 못했습니다.");
        }
      },

      onWebSocketClose: () => {
        if (!active) return;
        stopPresence();
        setConnected(false);
        setLoading(false);
        setError(
          (current) => current || "연결이 종료되었습니다. 다시 연결해 주세요.",
        );
      },
    });

    clientRef.current = client;
    client.activate();

    return () => {
      active = false;
      stopPresence();
      controller.abort();
      clientRef.current = null;
      void client.deactivate();
    };
  }, [
    roomId,
    user.id,
    useSecondServer,
    navigate,
    connectionAttempt,
    startPresence,
    stopPresence,
    refreshMembers,
  ]);
  async function loadOlderMessages() {
    if (!hasNext || nextCursor === null || olderLoadingRef.current) {
      return;
    }

    // 조회 중 다른 페이지로 이동했는지 확인할 기준
    const currentClient = clientRef.current;

    olderLoadingRef.current = true;
    setLoadingOlder(true);
    setError("");

    try {
      const data = await apiRequest(
        `/api/chat/rooms/${roomId}/messages?cursor=${nextCursor}`,
        {
          retryOn401: true,
          cache: "no-store",
        },
      );

      if (clientRef.current !== currentClient) return;
      data.messages.forEach((message) => {
        seenMessageIdsRef.current.add(String(message.messageId));
      });

      const list = messageListRef.current;

      if (list) {
        const listTop = list.getBoundingClientRect().top;

        // 현재 화면에 보이는 첫 번째 메시지를 기준점으로 기록
        const firstVisible = Array.from(
          list.querySelectorAll("[data-message-id]"),
        ).find((element) => element.getBoundingClientRect().bottom > listTop);

        if (firstVisible) {
          historyAnchorRef.current = {
            messageId: firstVisible.dataset.messageId,
            offset: firstVisible.getBoundingClientRect().top - listTop,
          };
        }
      }

      followBottomRef.current = false;

      setMessages((current) => mergeMessages(data.messages, current));

      setNextCursor(data.nextCursor);
      setHasNext(data.hasNext);
    } catch (err) {
      if (clientRef.current === currentClient) {
        setError(
          err.status === 403
            ? "이 채팅방에 접근할 권한이 없습니다."
            : err.message,
        );
      }
    } finally {
      olderLoadingRef.current = false;

      if (clientRef.current === currentClient) {
        setLoadingOlder(false);
      }
    }
  }
  async function kickMember(targetUserId, nickname) {
    if (
      !canKick ||
      kickingRef.current ||
      bansLoading ||
      bansError ||
      bannedUserIds.has(String(targetUserId))
    ) {
      return;
    }

    const reason = window.prompt(
      `${nickname}님을 강퇴합니다.\n사유를 입력하세요. 비워두어도 됩니다.`,
      "",
    );

    if (reason === null) return;

    if (reason.length > 500) {
      setError("강퇴 사유는 500자 이하로 입력해 주세요.");
      return;
    }

    kickingRef.current = true;
    setKickingUserId(targetUserId);
    setError("");

    try {
      await chatApi.kickMember(
        Number(roomId),
        targetUserId,
        reason.trim() || null,
      );

      // 목록 재조회가 끝날 때까지 중복 클릭 방지
      setBansLoading(true);
      setBanRevision((current) => current + 1);
    } catch (err) {
      if (err.status === 409) {
        setError("이미 강퇴된 회원입니다.");
        setBansLoading(true);
        setBanRevision((current) => current + 1);
      } else {
        setError(
          err.status === 403
            ? "강퇴 권한이 없거나 인증 상태가 변경되었습니다."
            : err.status === 404
              ? "현재 방에 참여 중인 회원이 아닙니다."
              : err.message,
        );
      }
    } finally {
      kickingRef.current = false;
      setKickingUserId(null);
    }
  }
  async function leaveRoom() {
    if (leaving) {
      return;
    }

    setLeaving(true);
    setError("");

    try {
      await chatApi.leaveRoom(Number(roomId));

      navigate("/");
    } catch (error) {
      setError(error.message);
    } finally {
      setLeaving(false);
    }
  }
  function sendMessage(event) {
    event.preventDefault();

    const text = content.trim();
    const client = clientRef.current;

    if (!text) return;

    if (!connected || !client?.connected) {
      setError("채팅 서버 연결을 확인해 주세요.");
      return;
    }

    try {
      followBottomRef.current = true;

      client.publish({
        destination: "/app/chat.send",
        headers: {
          "content-type": "application/json",
        },
        body: JSON.stringify({
          roomId: Number(roomId),
          content: text,
        }),
      });

      setContent("");
      setError("");
    } catch {
      setError("메시지를 전송하지 못했습니다.");
    }
  }

  return (
    <main className="chat-room-page">
      <section className="chat-room-shell" aria-label="채팅방">
        <header className="chat-room-header">
          <Link to="/chat">← 채팅방 목록</Link>

          <div className="chat-room-heading">
            <div>
              <h1>{room?.roomName ?? "채팅방"}</h1>
              <p>
                {user.nickname}님 · {connected ? "연결됨" : "연결 안 됨"}
              </p>
            </div>

            <div className="chat-room-actions">
              {canKick && (
                <Button
                  variant="outline"
                  aria-expanded={showBanPanel}
                  onClick={() => {
                    setShowMemberPanel(false);

                    if (!showBanPanel) {
                      setBansLoading(true);
                      setBanRevision((current) => current + 1);
                    }

                    setShowBanPanel((current) => !current);
                  }}
                >
                  {showBanPanel ? "강퇴 관리 닫기" : "강퇴 관리"}
                </Button>
              )}
              <Button
                variant="outline"
                aria-haspopup="dialog"
                aria-expanded={showMemberPanel}
                disabled={!connected}
                onClick={() => {
                  setShowBanPanel(false);
                  setShowMemberPanel(true);
                  refreshMembers();
                }}
              >
                참여자
                {!membersLoading && !membersError && ` ${members.length}`}
              </Button>

              <Button variant="outline" onClick={leaveRoom} disabled={leaving}>
                {leaving ? "나가는 중..." : "채팅방 나가기"}
              </Button>
            </div>
          </div>

          {loading && <p role="status">대화를 불러오는 중입니다.</p>}
          {error && <p role="alert">{error}</p>}

          {!connected && !loading && (
            <Button
              variant="outline"
              onClick={() => {
                setLoading(true);
                setConnectionAttempt((current) => current + 1);
              }}
            >
              다시 연결
            </Button>
          )}
        </header>

        {showMemberPanel && (
          <ChatMemberPanel
            key={roomId}
            members={members}
            loading={membersLoading}
            error={membersError}
            connected={connected}
            currentUserId={user.id}
            onClose={() => setShowMemberPanel(false)}
            onReload={refreshMembers}
          />
        )}

        {canKick && showBanPanel && (
          <ChatBanPanel
            key={roomId}
            roomId={Number(roomId)}
            bans={bans}
            loading={bansLoading}
            error={bansError}
            onClose={() => setShowBanPanel(false)}
            onReleased={(banId) => {
              setBans((current) =>
                current.filter((ban) => ban.banId !== banId),
              );
            }}
            onReload={() => {
              setBansLoading(true);
              setBanRevision((current) => current + 1);
            }}
          />
        )}
        <div className="chat-message-area">
          <div
            ref={messageListRef}
            className="chat-message-list"
            aria-label="채팅 메시지"
            tabIndex={0}
            onScroll={handleMessageScroll}
          >
            {!loading && hasNext && (
              <div className="chat-history-action">
                <Button
                  variant="outline"
                  onClick={loadOlderMessages}
                  disabled={loadingOlder}
                >
                  {loadingOlder ? "불러오는 중..." : "이전 메시지 불러오기"}
                </Button>
              </div>
            )}

            {!loading && !error && messages.length === 0 && (
              <p className="chat-empty">아직 대화가 없습니다.</p>
            )}

            {messages.map((message) => {
              const blocked = message.visibility !== "SHOW";
              const system = message.messageType !== "CHAT";
              const mine = String(message.userId) === String(user.id);

              if (system) {
                return (
                  <div
                    key={message.messageId}
                    data-message-id={message.messageId}
                    className="chat-system-message"
                  >
                    {blocked ? "공개되지 않은 알림입니다." : message.content}
                  </div>
                );
              }

              return (
                <article
                  key={message.messageId}
                  data-message-id={message.messageId}
                  className={`chat-message ${mine ? "mine" : "other"}`}
                >
                  <div className="chat-message-header">
                    <div className="chat-author">
                      <strong>{message.nickname}</strong>

                      {onlineUserIds.has(String(message.userId)) && (
                        <span
                          className="chat-online-dot"
                          role="img"
                          aria-label="현재 이 채팅방에 접속 중"
                          title="현재 이 채팅방에 접속 중"
                        />
                      )}
                    </div>

                    {canKick &&
                      !mine &&
                      (bannedUserIds.has(String(message.userId)) ? (
                        <span className="chat-kicked-label">강퇴됨</span>
                      ) : (
                        <button
                          type="button"
                          className="chat-kick-button"
                          onClick={() =>
                            kickMember(message.userId, message.nickname)
                          }
                          disabled={
                            kickingUserId !== null ||
                            bansLoading ||
                            Boolean(bansError)
                          }
                        >
                          {kickingUserId === message.userId
                            ? "처리 중…"
                            : "강퇴"}
                        </button>
                      ))}
                  </div>

                  <p className="chat-message-content">
                    {blocked
                      ? (BLOCK_MESSAGES[message.blockReason] ??
                        BLOCK_MESSAGES.DEFAULT)
                      : message.content}
                  </p>

                  <time
                    dateTime={message.createdAt}
                    className="chat-message-time"
                  >
                    {new Date(message.createdAt).toLocaleTimeString("ko-KR", {
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </time>
                </article>
              );
            })}
          </div>
          {isAwayFromBottom && (
            <div className="chat-scroll-controls">
              {newMessageCount > 0 && (
                <button
                  type="button"
                  className="chat-new-message-notice"
                  onClick={goToBottom}
                >
                  <span role="status">새 메시지 {newMessageCount}개</span>
                  <span aria-hidden="true">↓</span>
                </button>
              )}

              <button
                type="button"
                className="chat-scroll-bottom"
                onClick={goToBottom}
                aria-label="최신 메시지로 이동"
                title="최신 메시지로 이동"
              >
                <svg
                  viewBox="0 0 24 24"
                  width="22"
                  height="22"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  aria-hidden="true"
                >
                  <path d="m6 9 6 6 6-6" />
                </svg>
              </button>
            </div>
          )}
        </div>

        <form onSubmit={sendMessage} className="chat-input-form">
          <input
            className="chat-input"
            aria-label="채팅 메시지"
            placeholder={
              connected ? "메시지를 입력하세요" : "채팅 연결을 확인해 주세요"
            }
            value={content}
            onChange={(event) => setContent(event.target.value)}
            maxLength={2000}
            disabled={!connected || leaving}
          />

          <Button
            type="submit"
            variant="primary"
            disabled={!connected || leaving || !content.trim()}
          >
            전송
          </Button>
        </form>
      </section>
    </main>
  );
}
