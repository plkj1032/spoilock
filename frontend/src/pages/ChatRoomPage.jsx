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
import { apiRequest } from "../api/authApi";
import Button from "../components/common/Button";
import "../styles/ChatRoomPage.css";

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
  const bottomRef = useRef(null);
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
  const [showBanPanel, setShowBanPanel] = useState(false);
  const kickingRef = useRef(false);

  const canKick = Boolean(
    room &&
    (room.roomType === "DEFAULT"
      ? user.role === "ADMIN"
      : ["CUSTOM", "PRIVATE"].includes(room.roomType) &&
        String(room.createdById) === String(user.id)),
  );
  useLayoutEffect(() => {
    if (loading || error || initialScrollDoneRef.current) {
      return;
    }

    if (bottomRef.current) {
      bottomRef.current.scrollIntoView({
        behavior: "instant",
        block: "end",
      });

      initialScrollDoneRef.current = true;
    }
  }, [loading, error]);

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

        if (active) {
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

      onConnect: () => {
        if (!active) return;

        client.subscribe("/user/queue/chat", receiveMessage);

        setConnected(true);
        setError("");
        loadHistory();
      },

      onStompError: () => {
        if (active) {
          setError(
            "요청이 거절되었습니다. 로그인과 방 참여 상태를 확인해 주세요.",
          );
        }
      },

      onWebSocketError: () => {
        if (active) {
          setError("채팅 서버에 연결하지 못했습니다.");
        }
      },

      onWebSocketClose: () => {
        if (active) {
          setConnected(false);
          setLoading(false);
          setError("연결이 종료되었습니다. 페이지를 새로고침해 주세요.");
        }
      },
    });

    clientRef.current = client;
    client.activate();

    return () => {
      active = false;
      controller.abort();
      clientRef.current = null;
      void client.deactivate();
    };
  }, [roomId, user.id, useSecondServer, navigate]);
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
    if (!canKick || kickingRef.current) return;

    const reason = window.prompt(
      `${nickname}님을 강퇴합니다.\n사유를 입력하세요. 비워두어도 됩니다.`,
      "",
    );

    // 취소 버튼을 누르면 요청하지 않음
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
    } catch (err) {
      setError(
        err.status === 403
          ? "강퇴 권한이 없거나 인증 상태가 변경되었습니다."
          : err.message,
      );
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
    <main className="container main-content">
      <Link to="/chat">← 채팅방 목록</Link>

      <div className="section-top">
        <div>
          <span className="section-overline">CHAT ROOM</span>
          <h1>{room?.roomName ?? "채팅방"}</h1>
          <p>
            {user.nickname}님 · {connected ? "연결됨" : "연결 안 됨"}
          </p>
        </div>

        <Button variant="outline" onClick={leaveRoom} disabled={leaving}>
          {leaving ? "나가는 중..." : "채팅방 나가기"}
        </Button>
      </div>

      {loading && <p role="status">대화를 불러오는 중입니다.</p>}
      {error && <p role="alert">{error}</p>}
      {canKick && (
        <>
          <Button
            variant="outline"
            aria-expanded={showBanPanel}
            onClick={() => setShowBanPanel((current) => !current)}
          >
            {showBanPanel ? "강퇴 관리 닫기" : "강퇴 관리"}
          </Button>

          {showBanPanel && (
            <ChatBanPanel key={roomId} roomId={Number(roomId)} />
          )}
        </>
      )}

      {!loading && hasNext && (
        <Button
          variant="outline"
          onClick={loadOlderMessages}
          disabled={loadingOlder}
        >
          {loadingOlder ? "불러오는 중..." : "이전 메시지 불러오기"}
        </Button>
      )}
      <div className="chat-message-list" aria-label="채팅 메시지">
        {!loading && !error && messages.length === 0 && (
          <p>아직 대화가 없습니다.</p>
        )}

        {messages.map((message) => {
          const blocked = message.visibility !== "SHOW";
          const system = message.messageType !== "CHAT";
          const mine = message.userId === user.id;

          if (system) {
            return (
              <div key={message.messageId} className="chat-system-message">
                {blocked ? "공개되지 않은 알림입니다." : message.content}
              </div>
            );
          }

          return (
            <article
              key={message.messageId}
              className={`chat-message ${mine ? "mine" : "other"}`}
            >
              <div className="chat-message-header">
                <strong>{message.nickname}</strong>

                {canKick && String(message.userId) !== String(user.id) && (
                  <button
                    type="button"
                    onClick={() => kickMember(message.userId, message.nickname)}
                    disabled={kickingUserId !== null}
                  >
                    {kickingUserId === message.userId ? "처리 중..." : "강퇴"}
                  </button>
                )}
              </div>

              <p className="chat-message-content">
                {blocked
                  ? BLOCK_MESSAGES[message.blockReason] ?? BLOCK_MESSAGES.DEFAULT
                  : message.content}
              </p>

              <time dateTime={message.createdAt} className="chat-message-time">
                {new Date(message.createdAt).toLocaleTimeString("ko-KR", {
                  hour: "2-digit",
                  minute: "2-digit",
                })}
              </time>
            </article>
          );
        })}
      </div>

      <form onSubmit={sendMessage} className="chat-input-form">
        <input
          className="chat-input"
          aria-label="채팅 메시지"
          placeholder="메시지를 입력하세요"
          value={content}
          onChange={(event) => setContent(event.target.value)}
          maxLength={2000}
          disabled={!connected}
        />

        <Button
          type="submit"
          variant="primary"
          disabled={!connected || !content.trim()}
        >
          전송
        </Button>
      </form>
      <div ref={bottomRef} />
    </main>
  );
}
