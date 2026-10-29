import { useEffect, useId, useRef, useState } from "react";
import { chatInvitationApi } from "../../api/chatInvitationApi";
import "../../styles/ChatInvitationSender.css";

function InvitationForm({ target }) {
  const selectId = useId();
  const busyRef = useRef(false);
  const activeRef = useRef(false);

  const [rooms, setRooms] = useState([]);
  const [roomId, setRoomId] = useState("");
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState(null);
  const [copyMessage, setCopyMessage] = useState("");
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    activeRef.current = true;

    return () => {
      activeRef.current = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    const controller = new AbortController();

    chatInvitationApi
      .getRooms(controller.signal)
      .then((data) => {
        if (!active) return;

        setRooms(data);
        setError("");
      })
      .catch((err) => {
        if (active) {
          setError(err.message || "초대할 방을 불러오지 못했습니다.");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [revision]);

  async function sendInvitation(event) {
    event.preventDefault();

    if (!roomId || busyRef.current || result) return;

    busyRef.current = true;
    setSending(true);
    setError("");

    try {
      const data = await chatInvitationApi.send(Number(roomId), target.userId);

      if (activeRef.current) {
        setResult(data);
      }
    } catch (err) {
      if (activeRef.current) {
        setError(err.message || "초대 전송에 실패했습니다.");
      }
    } finally {
      busyRef.current = false;

      if (activeRef.current) {
        setSending(false);
      }
    }
  }

  async function copyCode() {
    try {
      await navigator.clipboard.writeText(result.code);

      if (activeRef.current) {
        setCopyMessage("초대코드를 복사했습니다.");
      }
    } catch {
      if (activeRef.current) {
        setCopyMessage(
          "자동 복사가 안 되면 아래 코드를 직접 선택해 복사해 주세요.",
        );
      }
    }
  }

  if (loading) {
    return (
      <p className="chat-invite-status" role="status">
        초대할 방을 불러오는 중…
      </p>
    );
  }

  if (result) {
    return (
      <div className="chat-invite-result">
        <strong>✉️ 초대 알림을 보냈어요</strong>

        <p>
          {target.nickname}님을 ‘{result.roomName}’에 초대했어요. 상대방은
          알림에서 초대를 수락할 수 있어요.
        </p>

        <label htmlFor={selectId}>초대코드</label>

        <input
          id={selectId}
          className="chat-invite-code"
          value={result.code}
          readOnly
          onFocus={(event) => event.target.select()}
        />

        <button
          type="button"
          className="chat-invite-secondary"
          onClick={copyCode}
        >
          코드 복사
        </button>

        {copyMessage && <p role="status">{copyMessage}</p>}

        <small>
          유효기간: {new Date(result.expiresAt).toLocaleString("ko-KR")}
          <br />
          초대받은 계정에서만 사용할 수 있어요. 코드는 이 화면을 닫기 전에
          복사해 주세요.
        </small>
      </div>
    );
  }

  return (
    <form className="chat-invite-form" onSubmit={sendInvitation}>
      {rooms.length > 0 ? (
        <>
          <label htmlFor={selectId}>초대할 비공개방</label>

          <select
            id={selectId}
            value={roomId}
            onChange={(event) => {
              setRoomId(event.target.value);
              setError("");
            }}
            disabled={sending}
            required
          >
            <option value="">채팅방을 선택하세요</option>

            {rooms.map((room) => (
              <option key={room.roomId} value={room.roomId}>
                {room.roomName} · {room.dramaTitle}
              </option>
            ))}
          </select>

          <p className="chat-invite-help">
            내가 방장인 비공개방에 초대할 수 있어요. 초대는 24시간 동안
            유효해요.
          </p>

          <button
            type="submit"
            className="chat-invite-primary"
            disabled={sending || !roomId}
          >
            {sending ? "초대 보내는 중…" : "초대 보내기"}
          </button>
        </>
      ) : (
        !error && (
          <p className="chat-invite-status">
            초대할 비공개방이 없어요. 먼저 비공개방을 만들어 주세요.
          </p>
        )
      )}

      {error && (
        <div className="chat-invite-error" role="alert">
          <p>{error}</p>

          {rooms.length === 0 && (
            <button
              type="button"
              className="chat-invite-secondary"
              onClick={() => {
                setLoading(true);
                setError("");
                setRevision((value) => value + 1);
              }}
            >
              다시 불러오기
            </button>
          )}
        </div>
      )}
    </form>
  );
}

export default function ChatInvitationSender({ target }) {
  const [opened, setOpened] = useState(false);
  const panelId = useId();

  return (
    <section className="chat-invite-sender">
      <button
        type="button"
        className="chat-invite-toggle"
        aria-expanded={opened}
        aria-controls={panelId}
        onClick={() => setOpened((value) => !value)}
      >
        ✉️ 채팅방 초대
        <span aria-hidden="true">{opened ? "▴" : "▾"}</span>
      </button>

      <div id={panelId} hidden={!opened}>
        {opened && (
          <div className="chat-invite-panel">
            <InvitationForm key={target.userId} target={target} />
          </div>
        )}
      </div>
    </section>
  );
}
