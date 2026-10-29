import { useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { chatApi } from "../api/chatApi";
import useRoomPage from "../hooks/useRoomPage";
import Pagination from "../components/common/Pagination";
import DefaultRoomCrown from "../components/common/DefaultRoomCrown";

import "../styles/AdminCenter.css";

export default function AdminChatPage({ user }) {
  const navigate = useNavigate();
  const enteringRef = useRef(false);

  const [query, setQuery] = useState("");
  const [filters, setFilters] = useState({ keyword: "", page: 0 });
  const [enteringId, setEnteringId] = useState(null);
  const [actionError, setActionError] = useState("");

  const { rooms, page, totalPages, totalElements, loading, error, refresh } =
    useRoomPage({
      userId: user.id,
      scope: "public",
      page: filters.page,
      size: 10,
      keyword: filters.keyword,
      roomType: "DEFAULT",
      sort: "name",
    });

  function handleSearch(event) {
    event.preventDefault();
    setFilters({ keyword: query.trim(), page: 0 });
  }

  async function enterRoom(roomId) {
    if (enteringRef.current) return;

    enteringRef.current = true;
    setEnteringId(roomId);
    setActionError("");

    try {
      await chatApi.enterRoom(roomId);
      navigate(`/chat/${roomId}`);
    } catch (err) {
      setActionError(err.message || "토론방에 입장하지 못했습니다.");
    } finally {
      enteringRef.current = false;
      setEnteringId(null);
    }
  }

  return (
    <main className="container main-content admin-center">
      <Link to="/admin" className="admin-center-back">
        ← 관리자 센터
      </Link>

      <header className="admin-center-heading">
        <span className="section-overline">CHAT MANAGEMENT</span>
        <h1>채팅방 관리센터</h1>
        <p>
          공개된 드라마의 기본 토론방입니다. 입장 후 참여자·강퇴·신고 관리
          기능을 이용할 수 있습니다.
        </p>
      </header>

      <form className="admin-center-search" onSubmit={handleSearch}>
        <input
          type="search"
          aria-label="드라마 또는 토론방 이름 검색"
          placeholder="드라마 또는 토론방 이름으로 검색"
          value={query}
          maxLength={200}
          onChange={(event) => setQuery(event.target.value)}
        />
        <button type="submit" className="button button-primary">
          검색
        </button>
      </form>

      <div className="admin-center-list-heading">
        <span>기본 토론방 {loading ? "조회 중" : `${totalElements}개`}</span>
        <button
          type="button"
          className="button button-outline"
          onClick={refresh}
          disabled={loading}
        >
          새로고침
        </button>
      </div>

      {(error || actionError) && (
        <p className="form-error" role="alert">
          {actionError || error}
        </p>
      )}

      <section aria-label="기본 토론방 목록" aria-busy={loading}>
        {loading ? (
          <p className="admin-center-empty">토론방을 불러오는 중입니다.</p>
        ) : error ? null : rooms.length === 0 ? (
          <p className="admin-center-empty">
            {filters.keyword
              ? "검색 조건에 맞는 토론방이 없습니다."
              : "현재 관리할 기본 토론방이 없습니다."}
          </p>
        ) : (
          <div className="admin-center-room-list">
            {rooms.map((room) => (
              <article className="admin-center-room" key={room.roomId}>
                <span className="admin-center-room-icon" aria-hidden="true">
                  <DefaultRoomCrown />
                </span>

                <div className="admin-center-room-info">
                  <span className="admin-center-room-label">기본 토론방</span>
                  <h2>{room.roomName}</h2>
                  <p>참여 회원 {room.memberCount ?? 0}명</p>
                </div>

                <button
                  type="button"
                  className="button button-outline"
                  disabled={enteringId !== null}
                  onClick={() => enterRoom(room.roomId)}
                >
                  {enteringId === room.roomId ? "입장 중..." : "입장 및 관리"}
                </button>
              </article>
            ))}
          </div>
        )}
      </section>

      <Pagination
        page={page}
        totalPages={totalPages}
        onPageChange={(next) =>
          setFilters((current) => ({ ...current, page: next }))
        }
        disabled={loading || enteringId !== null}
        label="관리자 토론방 목록 페이지"
      />
    </main>
  );
}
