import { useRef, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";

import { chatApi } from "../api/chatApi";
import DramaGallery from "../components/drama/DramaGallery";
import ChatRoomCreate from "../components/chat/ChatRoomCreate";
import Pagination from "../components/common/Pagination";
import useRoomPage from "../hooks/useRoomPage";

import "../styles/HomePage.css";
import "../styles/ChatRoomList.css";

export default function HomePage({ user, onLogin, onNotice }) {
  const navigate = useNavigate();
  const enteringRef = useRef(false);

  const [requestedPage, setRequestedPage] = useState(0);
  const [selectedDrama, setSelectedDrama] = useState(null);
  const [enteringRoomId, setEnteringRoomId] = useState(null);
  const [createRoomOpen, setCreateRoomOpen] = useState(false);

  const {
    rooms,
    page,
    totalPages,
    totalElements,
    loading: roomsLoading,
    error: roomsError,
    refresh,
  } = useRoomPage({
    userId: user?.id,
    scope: "public",
    page: requestedPage,
    size: 5,
    dramaId: selectedDrama?.id ?? "",
    sort: "members",
  });

  function selectDrama(drama) {
    setSelectedDrama(drama);
    setRequestedPage(0);
  }

  function showAllRooms() {
    setSelectedDrama(null);
    setRequestedPage(0);
  }

  async function enterRoom(roomId) {
    if (!user) {
      onLogin();
      return;
    }

    if (enteringRef.current) return;

    enteringRef.current = true;
    setEnteringRoomId(roomId);

    try {
      await chatApi.enterRoom(roomId);
      navigate(`/chat/${roomId}`);
    } catch (error) {
      onNotice(`채팅방 입장 실패: ${error.message}`);
    } finally {
      enteringRef.current = false;
      setEnteringRoomId(null);
    }
  }

  function createRoom() {
    if (!user) {
      onLogin();
      return;
    }

    setCreateRoomOpen(true);
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
        selectedDramaId={selectedDrama?.id}
        onSelectDrama={selectDrama}
      />

      <div className="section-top rooms-title">
        <div>
          <span className="section-overline">LIVE ROOMS</span>

          <h2>
            {selectedDrama ? `${selectedDrama.title} 채팅방` : "실시간 채팅방"}
          </h2>

          {selectedDrama && (
            <button
              type="button"
              className="room-filter-reset"
              onClick={showAllRooms}
            >
              전체 채팅방 보기
            </button>
          )}
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
        <div>
          <div className="room-list" aria-busy={roomsLoading}>
            {roomsLoading && <p role="status">채팅방을 불러오는 중입니다.</p>}

            {roomsError && (
              <div role="alert">
                <p>{roomsError}</p>

                <button
                  type="button"
                  className="button button-outline"
                  onClick={refresh}
                >
                  다시 불러오기
                </button>
              </div>
            )}

            {!roomsLoading && !roomsError && rooms.length === 0 && (
              <p>현재 참여할 수 있는 채팅방이 없습니다.</p>
            )}

            {!roomsLoading &&
              rooms.map((room) => (
                <button
                  type="button"
                  className="room-card"
                  key={room.roomId}
                  onClick={() => enterRoom(room.roomId)}
                  disabled={enteringRoomId !== null}
                >
                  <span className="room-icon" aria-hidden="true">
                    ▢
                  </span>

                  <span className="room-copy">
                    <strong>{room.roomName}</strong>

                    {room.description && <small>{room.description}</small>}
                  </span>

                  <span className="room-members">
                    가입 {room.memberCount ?? 0}명
                  </span>

                  <span className="room-enter">
                    {enteringRoomId === room.roomId ? "입장 중..." : "입장 ›"}
                  </span>
                </button>
              ))}
          </div>

          {!roomsLoading && totalElements > 0 && (
            <>
              <Pagination
                page={page}
                totalPages={totalPages}
                onPageChange={setRequestedPage}
                disabled={enteringRoomId !== null}
                label="메인 채팅방 페이지"
              />

              <p className="demo-disclaimer" role="status">
                전체 {totalElements}개 · {page + 1} / {totalPages}페이지
              </p>
            </>
          )}
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
        채팅방과 드라마 정보는 실제 서버 데이터와 연결되어 있습니다.
      </p>

      {createRoomOpen && (
        <ChatRoomCreate
          onClose={() => {
            setCreateRoomOpen(false);
            refresh();
          }}
        />
      )}
    </main>
  );
}
