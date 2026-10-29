import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../../styles/ChatRoomCreate.css";
import ChatFollowingInviteModal from "./ChatFollowingInviteModal";
import { chatApi } from "../../api/chatApi";
import { dramaApi } from "../../api/dramaApi";
import Button from "../common/Button";

export default function ChatRoomCreate({ onClose }) {
  const navigate = useNavigate();

  const [dramas, setDramas] = useState([]);
  const [createdPrivateRoom, setCreatedPrivateRoom] = useState(null);
  const [dramaId, setDramaId] = useState("");
  const [roomName, setRoomName] = useState("");
  const [description, setDescription] = useState("");
  const [roomType, setRoomType] = useState("CUSTOM");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    dramaApi
      .getOptions()
      .then((data) => {
        if (active) {
          setDramas(data ?? []);
        }
      })
      .catch((error) => {
        if (active) {
          setError(error.message);
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  async function handleSubmit(event) {
    event.preventDefault();

    if (!dramaId) {
      setError("작품을 선택해 주세요.");
      return;
    }

    const trimmedRoomName = roomName.trim();

    if (!trimmedRoomName) {
      setError("채팅방 이름을 입력해 주세요.");
      return;
    }

    if (submitting) {
      return;
    }

    setSubmitting(true);
    setError("");

    try {
      const createdRoom = await chatApi.createRoom(
        Number(dramaId),
        trimmedRoomName,
        description.trim() || null,
        roomType,
      );

      if (roomType === "PRIVATE") {
        setCreatedPrivateRoom(createdRoom);
        return;
      }

      onClose();
      navigate(`/chat/${createdRoom.roomId}`);
    } catch (error) {
      setError(error.message);
    } finally {
      setSubmitting(false);
    }
  }

  function handleBackdropClick(event) {
    if (event.target === event.currentTarget && !submitting) {
      onClose();
    }
  }
  if (createdPrivateRoom) {
    return (
      <ChatFollowingInviteModal
        key={createdPrivateRoom.roomId}
        roomId={createdPrivateRoom.roomId}
        roomName={createdPrivateRoom.roomName}
        onClose={onClose}
        onEnter={() => {
          onClose();
          navigate(`/chat/${createdPrivateRoom.roomId}`);
        }}
      />
    );
  }

  return (
    <div className="chat-create-backdrop" onMouseDown={handleBackdropClick}>
      <section
        className="chat-create-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="chat-create-title"
      >
        <header className="chat-create-header">
          <div>
            <span className="section-overline">CREATE ROOM</span>
            <h2 id="chat-create-title">채팅방 만들기</h2>
            <p>함께 이야기할 작품을 선택하고 채팅방을 만들어보세요.</p>
          </div>

          <button
            type="button"
            className="chat-create-close"
            onClick={onClose}
            disabled={submitting}
            aria-label="채팅방 만들기 닫기"
          >
            ×
          </button>
        </header>

        {loading ? (
          <div className="chat-create-state">
            작품 목록을 불러오는 중입니다.
          </div>
        ) : dramas.length === 0 ? (
          <div className="chat-create-state">
            채팅방을 만들 수 있는 작품이 없습니다.
          </div>
        ) : (
          <form className="chat-create-form" onSubmit={handleSubmit}>
            <div className="chat-create-field">
              <label htmlFor="chat-room-type">공개 설정</label>

              <select
                id="chat-room-type"
                value={roomType}
                onChange={(event) => setRoomType(event.target.value)}
                disabled={submitting}
                aria-describedby="chat-room-type-help"
              >
                <option value="CUSTOM">공개 채팅방</option>
                <option value="PRIVATE">비공개 채팅방</option>
              </select>

              <small id="chat-room-type-help"></small>
            </div>
            <div className="chat-create-field">
              <label htmlFor="chat-drama">작품</label>

              <select
                id="chat-drama"
                value={dramaId}
                onChange={(event) => setDramaId(event.target.value)}
                disabled={submitting}
              >
                <option value="">작품을 선택하세요</option>

                {dramas.map((drama) => (
                  <option key={drama.dramaId} value={drama.dramaId}>
                    {drama.title}
                    {drama.totalEpisodeCount
                      ? ` · ${drama.totalEpisodeCount}부작`
                      : ""}
                  </option>
                ))}
              </select>
            </div>

            <div className="chat-create-field">
              <label htmlFor="chat-room-name">채팅방 이름</label>

              <input
                id="chat-room-name"
                type="text"
                value={roomName}
                onChange={(event) => setRoomName(event.target.value)}
                maxLength={100}
                disabled={submitting}
                placeholder="예) 8화까지 본 사람들 모여요"
              />

              <span className="chat-create-count">{roomName.length} / 100</span>
            </div>

            <div className="chat-create-field">
              <label htmlFor="chat-room-description">
                채팅방 설명
                <span>선택</span>
              </label>

              <textarea
                id="chat-room-description"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                disabled={submitting}
                placeholder="어떤 이야기를 나누는 방인지 간단히 설명해 주세요."
                rows={4}
              />
            </div>

            {error && (
              <p className="chat-create-error" role="alert">
                {error}
              </p>
            )}

            <div className="chat-create-actions">
              <Button variant="outline" onClick={onClose} disabled={submitting}>
                취소
              </Button>

              <Button
                type="submit"
                variant="primary"
                disabled={submitting || !dramaId || !roomName.trim()}
              >
                {submitting ? "채팅방 만드는 중..." : "채팅방 만들기"}
              </Button>
            </div>
          </form>
        )}
      </section>
    </div>
  );
}
