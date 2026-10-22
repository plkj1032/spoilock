import { useEffect, useRef, useState, useLayoutEffect } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { Client } from "@stomp/stompjs";

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

  const [searchParams] = useSearchParams();

  // 개발 환경에서만 두 번째 서버 연결 허용
  const useSecondServer =
    import.meta.env.DEV && searchParams.get("server") === "8081";

  const clientRef = useRef(null);
  const bottomRef = useRef(null);
  const initialScrollDoneRef = useRef(false);

  const [messages, setMessages] = useState([]);
  const [content, setContent] = useState("");
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [nextCursor, setNextCursor] = useState(null);
  const [hasNext, setHasNext] = useState(false);
  const [loadingOlder, setLoadingOlder] = useState(false);
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
    const controller = new AbortController();

    async function getJson(path) {
      const response = await fetch(path, {
        credentials: "include",
        cache: "no-store",
        signal: controller.signal,
      });

      if (response.status === 401) {
        throw new Error("로그인이 만료되었습니다.");
      }

      if (response.status === 403) {
        throw new Error("이 채팅방에 접근할 권한이 없습니다.");
      }

      if (!response.ok) {
        throw new Error("대화를 불러오지 못했습니다.");
      }

      return response.json();
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
  }, [roomId, user.id, useSecondServer]);

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
      const response = await fetch(
        `/api/chat/rooms/${roomId}/messages?cursor=${nextCursor}`,
        {
          credentials: "include",
          cache: "no-store",
        },
      );

      if (response.status === 401) {
        throw new Error("로그인이 만료되었습니다.");
      }

      if (response.status === 403) {
        throw new Error("이 채팅방에 접근할 권한이 없습니다.");
      }

      if (!response.ok) {
        throw new Error("이전 대화를 불러오지 못했습니다.");
      }

      const data = await response.json();

      if (clientRef.current !== currentClient) return;

      setMessages((current) => mergeMessages(data.messages, current));

      setNextCursor(data.nextCursor);
      setHasNext(data.hasNext);
    } catch (err) {
      if (clientRef.current === currentClient) {
        setError(err.message);
      }
    } finally {
      olderLoadingRef.current = false;

      if (clientRef.current === currentClient) {
        setLoadingOlder(false);
      }
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
          <h1>채팅방 {roomId}</h1>
          <p>
            {user.nickname}님 · {connected ? "연결됨" : "연결 안 됨"}
          </p>
        </div>
      </div>

      {loading && <p role="status">대화를 불러오는 중입니다.</p>}
      {error && <p role="alert">{error}</p>}

      {!loading && hasNext && (
        <button
          type="button"
          className="button button-outline"
          onClick={loadOlderMessages}
          disabled={loadingOlder}
        >
          {loadingOlder ? "불러오는 중..." : "이전 메시지 불러오기"}
        </button>
      )}
      <div
        aria-label="채팅 메시지"
        style={{
          display: "grid",
          gap: 12,
          padding: "20px 0",
          minHeight: 200,
        }}
      >
        {!loading && !error && messages.length === 0 && (
          <p>아직 대화가 없습니다.</p>
        )}

        {messages.map((message) => {
          const blocked = message.visibility !== "SHOW";
          const system = message.messageType !== "CHAT";
          const mine = message.userId === user.id;

          if (system) {
            return (
              <div
                key={message.messageId}
                style={{ textAlign: "center", color: "#64748b" }}
              >
                {blocked ? "공개되지 않은 알림입니다." : message.content}
              </div>
            );
          }

          return (
            <article
              key={message.messageId}
              style={{
                justifySelf: mine ? "end" : "start",
                maxWidth: "80%",
                padding: "12px 16px",
                borderRadius: 16,
                background: mine ? "#e0e7ff" : "#f1f5f9",
                color: "#1e293b",
                overflowWrap: "anywhere",
              }}
            >
              <strong>{message.nickname}</strong>

              <p style={{ margin: "8px 0", whiteSpace: "pre-wrap" }}>
                {blocked
                  ? "🔒 아직 공개되지 않은 메시지입니다."
                  : message.content}
              </p>

              <time
                dateTime={message.createdAt}
                style={{ fontSize: 12, color: "#64748b" }}
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

      <form
        onSubmit={sendMessage}
        style={{ display: "flex", gap: 12, paddingBottom: 24 }}
      >
        <input
          aria-label="채팅 메시지"
          placeholder="메시지를 입력하세요"
          value={content}
          onChange={(event) => setContent(event.target.value)}
          maxLength={2000}
          disabled={!connected}
          style={{ flex: 1, minWidth: 0, padding: 12 }}
        />

        <button
          type="submit"
          className="button button-primary"
          disabled={!connected || !content.trim()}
        >
          전송
        </button>
      </form>
      <div ref={bottomRef} />
    </main>
  );
}
