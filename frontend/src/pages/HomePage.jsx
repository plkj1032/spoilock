import { useRef, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";

import { chatApi } from "../api/chatApi";
import DramaGallery from "../components/drama/DramaGallery";
import ChatRoomCreate from "../components/chat/ChatRoomCreate";
import Pagination from "../components/common/Pagination";
import DefaultRoomCrown from "../components/common/DefaultRoomCrown";
import HomeFollowingFeed from "../components/feed/HomeFollowingFeed";
import useRoomPage from "../hooks/useRoomPage";

import "../styles/ChatRoomList.css";
import "../styles/HomePage.css";
import HomeFeedComposeModal from "../components/feed/HomeFeedComposeModal";

export default function HomePage({ user, onLogin, onNotice }) {
  const navigate = useNavigate();
  const enteringRef = useRef(false);
  const [createPostOpen, setCreatePostOpen] = useState(false);
  const [followingRevision, setFollowingRevision] = useState(0);
  const [requestedPage, setRequestedPage] = useState(0);
  const [selectedDrama, setSelectedDrama] = useState(null);
  const [enteringRoomId, setEnteringRoomId] = useState(null);
  const [createRoomOpen, setCreateRoomOpen] = useState(false);
  const [feedSummary, setFeedSummary] = useState(null);

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
    sort: selectedDrama ? "defaultFirst" : "members",
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

  function createPost() {
    if (!user) {
      onLogin();
      return;
    }

    setCreatePostOpen(true);
  }

  const communityParams = new URLSearchParams({
    tab: "FOLLOWING",
  });

  if (selectedDrama) {
    communityParams.set("dramaId", String(selectedDrama.id));
  }

  const communityUrl = `/community?${communityParams}`;
  const feedSummaryKey = `${user?.id ?? "guest"}:${selectedDrama?.id ?? "all"}:${followingRevision}`;

  const currentFeedSummary =
    feedSummary?.key === feedSummaryKey ? feedSummary : null;

  const feedCountText = !user
    ? "로그인 후 확인"
    : !currentFeedSummary
      ? "조회 중…"
      : currentFeedSummary.error || currentFeedSummary.totalElements == null
        ? "개수 조회 실패"
        : `전체 ${currentFeedSummary.totalElements}개`;

  return (
    <main className="container main-content home-page">
      <div className="section-top">
        <div>
          <span className="section-overline">NOW STREAMING</span>
          <h1>지금 시청중인 드라마</h1>
        </div>

        <div
          style={{
            display: "flex",
            alignItems: "center",
            flexWrap: "wrap",
            gap: "12px",
          }}
        >
          {selectedDrama && (
            <button
              type="button"
              className="room-filter-reset"
              onClick={showAllRooms}
              aria-label={`${selectedDrama.title} 선택 해제`}
            >
              선택 해제 ×
            </button>
          )}

          <NavLink to="/dramas" className="section-link">
            전체 보기 ›
          </NavLink>
        </div>
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

      <div className="home-social-grid">
        {/* 왼쪽: 실시간 채팅방 */}
        <section
          className="home-social-column"
          aria-labelledby="home-rooms-title"
        >
          <header className="home-column-heading">
            <div>
              <span className="section-overline">LIVE ROOMS</span>

              <h2 id="home-rooms-title">
                {selectedDrama
                  ? `${selectedDrama.title} 채팅방`
                  : "실시간 채팅방"}
              </h2>
            </div>

            <button
              type="button"
              className="button button-outline home-create-room"
              onClick={createRoom}
            >
              ＋ 방 만들기
            </button>
          </header>

          <div className="home-column-toolbar">
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "6px",
              }}
            >
              <span>
                {selectedDrama ? "이 드라마의 채팅방" : "전체 드라마의 채팅방"}
              </span>

              <span>
                {roomsLoading ? "조회 중…" : `전체 ${totalElements}개`}
              </span>
            </div>

            <NavLink to="/chat" className="section-link">
              전체 보기 ›
            </NavLink>
          </div>

          <div className="room-list" aria-busy={roomsLoading}>
            {roomsLoading && (
              <div className="home-column-empty" role="status">
                채팅방을 불러오는 중입니다.
              </div>
            )}

            {roomsError && (
              <div className="home-column-empty" role="alert">
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
              <div className="home-column-empty">
                현재 참여할 수 있는 채팅방이 없습니다.
              </div>
            )}

            {!roomsLoading &&
              rooms.map((room) => (
                <button
                  type="button"
                  className={`room-card ${
                    room.roomType === "DEFAULT" ? "is-default-room" : ""
                  }`}
                  key={room.roomId}
                  onClick={() => enterRoom(room.roomId)}
                  disabled={enteringRoomId !== null}
                >
                  {room.roomType === "DEFAULT" ? (
                    <DefaultRoomCrown />
                  ) : (
                    <span className="room-icon" aria-hidden="true">
                      ▢
                    </span>
                  )}

                  <span className="room-copy">
                    <strong>{room.roomName}</strong>

                    {room.description && <small>{room.description}</small>}
                  </span>

                  <span className="room-members">
                    {room.memberCount ?? 0}명
                  </span>

                  <span className="room-enter">
                    {enteringRoomId === room.roomId ? "입장 중..." : "입장 ›"}
                  </span>
                </button>
              ))}
          </div>

          {!roomsLoading && totalElements > 0 && (
            <Pagination
              page={page}
              totalPages={totalPages}
              onPageChange={setRequestedPage}
              disabled={enteringRoomId !== null}
              label="메인 채팅방 페이지"
            />
          )}
        </section>

        {/* 오른쪽: 팔로잉 커뮤니티 */}
        <section
          className="home-social-column"
          aria-labelledby="home-following-title"
        >
          <header className="home-column-heading">
            <div>
              <span className="section-overline">FOLLOWING FEEDS</span>

              <h2 id="home-following-title">
                {selectedDrama ? `${selectedDrama.title} 커뮤니티` : "커뮤니티"}
              </h2>
            </div>

            <button
              type="button"
              className="button button-outline home-create-room"
              onClick={createPost}
            >
              ＋ 글 작성하기
            </button>
          </header>

          <div className="home-column-toolbar">
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "6px",
              }}
            >
              <span>
                {selectedDrama
                  ? "이 드라마의 팔로잉 이야기"
                  : "팔로잉 최신 이야기"}
              </span>

              <span role="status">{feedCountText}</span>
            </div>

            <NavLink to={communityUrl} className="section-link">
              전체 보기 ›
            </NavLink>
          </div>

          <HomeFollowingFeed
            key={feedSummaryKey}
            user={user}
            onLogin={onLogin}
            dramaId={selectedDrama?.id ?? null}
            summaryKey={feedSummaryKey}
            onSummaryChange={setFeedSummary}
          />
        </section>
      </div>

      {createRoomOpen && (
        <ChatRoomCreate
          onClose={() => {
            setCreateRoomOpen(false);
            refresh();
          }}
        />
      )}
      {createPostOpen && user && (
        <HomeFeedComposeModal
          user={user}
          drama={selectedDrama}
          onLogin={onLogin}
          onNotice={onNotice}
          onClose={() => setCreatePostOpen(false)}
          onCreated={() => {
            setCreatePostOpen(false);
            setFollowingRevision((value) => value + 1);
          }}
        />
      )}
    </main>
  );
}
