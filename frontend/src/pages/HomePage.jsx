import { useEffect, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";

import { chatApi } from "../api/chatApi";
import DramaCard from "../components/drama/DramaCard";
import { NavLink } from "react-router-dom";
import DramaGallery from "../components/drama/DramaGallery";

export default function HomePage({ user, onLogin, onNotice }) {
  const navigate = useNavigate();

  const [rooms, setRooms] = useState([]);
  const [roomsLoading, setRoomsLoading] = useState(true);
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
          setRoomsLoading(false);
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

  function createRoom() {
    if (!user) {
      onLogin();
      return;
    }

    onNotice("채팅방 만들기는 다음 단계에서 연결할 예정입니다.");
  }
  return (
    <main className="container main-content">
      <div className="section-top">
        <div>
          <span className="section-overline">NOW STREAMING</span>
          <h1>지금 시청중인 드라마</h1>
        </div>
        <NavLink to="/dramas" className="section-link">
          전체 보기 ›
        </NavLink>
      </div>
      <DramaGallery
        key={user?.id ?? "guest"}
        user={user}
        onLogin={onLogin}
        onNotice={onNotice}
        limit={4}
      />
      <div className="section-top rooms-title">
        <div>
          <span className="section-overline">LIVE ROOMS</span>
          <h2>실시간 채팅방</h2>
        </div>
        <button
          type="button"
          className="button button-outline"
          onClick={createRoom}
        >
          ＋ 채팅방 만들기
        </button>
      </div>
      <div className="room-layout">
        <div className="room-list">
          {roomsLoading && <p>채팅방을 불러오는 중입니다.</p>}

          {!roomsLoading && rooms.length === 0 && (
            <p>현재 참여할 수 있는 채팅방이 없습니다.</p>
          )}

          {!roomsLoading &&
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
        <aside className="safety-card">
          <span className="safety-icon" aria-hidden="true">
            ♢
          </span>
          <span className="section-overline">SPOILOCK SAFE</span>
          <h3>안심하고 대화하세요</h3>
          <p>
            설정한 시청 진도 이후의 내용은 AI가 자동으로 감지하고 보호하는
            서비스를 만들고 있어요.
          </p>
          <div className="safety-bars">
            <span />
            <span />
            <span />
          </div>
          <small>
            현재 보호 수준 <strong>개발 중</strong>
          </small>
        </aside>
      </div>
      <p className="demo-disclaimer">
        드라마·평점 정보는 아직 프론트엔드 데모 데이터이며, 채팅방은 실제 서버
        데이터와 연결되어 있습니다.
      </p>
    </main>
  );
}
