import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { chatApi } from "../api/chatApi";

import "../styles/ChatPage.css";
import "../styles/ChatRoomList.css";

export default function ChatPage({ user, onLogin, onNotice }) {
  const navigate = useNavigate();

  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [enteringRoomId, setEnteringRoomId] = useState(null);

  useEffect(() => {
    let active = true;

    chatApi
      .getRooms()
      .then((data) => {
        if (active) {
          setRooms(data ?? []);
        }
      })
      .catch((error) => {
        if (active) {
          onNotice(`채팅방 목록 조회 실패: ${error.message}`);
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
  }, [onNotice]);

  async function enterRoom(roomId) {
    if (!user) {
      onLogin();
      return;
    }

    if (enteringRoomId !== null) {
      return;
    }

    setEnteringRoomId(roomId);

    try {
      await chatApi.enterRoom(roomId);

      navigate(`/chat/${roomId}`);
    } catch (error) {
      onNotice(`채팅방 입장 실패: ${error.message}`);
    } finally {
      setEnteringRoomId(null);
    }
  }

  return (
    <main className="container main-content">
      <span className="section-overline">LIVE ROOMS</span>
      <h1>채팅방</h1>

      <p className="page-subtitle">현재 참여할 수 있는 실시간 채팅방입니다.</p>

      <div className="room-list standalone-rooms">
        {loading && <p>채팅방을 불러오는 중입니다.</p>}

        {!loading && rooms.length === 0 && (
          <p>현재 참여할 수 있는 채팅방이 없습니다.</p>
        )}

        {!loading &&
          rooms.map((room) => (
            <button
              type="button"
              className="room-card"
              key={room.roomId}
              onClick={() => enterRoom(room.roomId)}
              disabled={enteringRoomId === room.roomId}
            >
              <span className="room-icon" aria-hidden="true">
                ▢
              </span>

              <span className="room-copy">
                <strong>{room.roomName}</strong>

                {room.description && <small>{room.description}</small>}
              </span>

              <span className="room-members">♧ {room.memberCount}명</span>

              <span className="room-enter">
                {enteringRoomId === room.roomId ? "입장 중..." : "입장 ›"}
              </span>
            </button>
          ))}
      </div>
    </main>
  );
}
