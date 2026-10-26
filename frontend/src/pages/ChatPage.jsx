import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";

import { chatApi } from "../api/chatApi";
import { dramaApi } from "../api/dramaApi";
import Button from "../components/common/Button";
import Pagination from "../components/common/Pagination";
import ChatRoomCreate from "../components/chat/ChatRoomCreate";
import useRoomPage from "../hooks/useRoomPage";

import "../styles/ChatPage.css";

const ROOM_TYPES = [
  { value: "ALL", label: "전체" },
  { value: "DEFAULT", label: "기본 토론방" },
  { value: "CUSTOM", label: "자유 채팅방" },
  { value: "PRIVATE", label: "비공개방" },
];

const TYPE_LABELS = {
  DEFAULT: "기본 토론방",
  CUSTOM: "자유 채팅방",
  PRIVATE: "비공개방",
};

function readFilters() {
  try {
    const saved = JSON.parse(
      sessionStorage.getItem("spoilock.chatListFilters") || "{}",
    );

    return {
      query: typeof saved?.query === "string" ? saved.query.slice(0, 200) : "",
      dramaId:
        typeof saved?.dramaId === "string" && /^[1-9]\d*$/.test(saved.dramaId)
          ? saved.dramaId
          : "",
      roomType: ROOM_TYPES.some((type) => type.value === saved?.roomType)
        ? saved.roomType
        : "ALL",
      sort: saved?.sort === "name" ? "name" : "members",
    };
  } catch {
    return {
      query: "",
      dramaId: "",
      roomType: "ALL",
      sort: "members",
    };
  }
}

function RoomPoster({ src, title }) {
  const [failed, setFailed] = useState(false);
  const initial = Array.from(title || "?")[0];

  return (
    <span className="chat-directory-poster" aria-hidden="true">
      {src && !failed ? (
        <img src={src} alt="" loading="lazy" onError={() => setFailed(true)} />
      ) : (
        <span>{initial}</span>
      )}
    </span>
  );
}

export default function ChatPage({ user, onLogin, onNotice }) {
  const navigate = useNavigate();
  const enteringRef = useRef(false);
  const searchRef = useRef(null);

  const [initialFilters] = useState(readFilters);

  const [query, setQuery] = useState(initialFilters.query);
  const [filters, setFilters] = useState({
    keyword: initialFilters.query,
    dramaId: initialFilters.dramaId,
    roomType: initialFilters.roomType,
    sort: initialFilters.sort,
    page: 0,
  });

  const [dramas, setDramas] = useState([]);
  const [dramasLoading, setDramasLoading] = useState(true);
  const [dramaError, setDramaError] = useState("");
  const [dramaRevision, setDramaRevision] = useState(0);

  const [enteringRoomId, setEnteringRoomId] = useState(null);
  const [createRoomOpen, setCreateRoomOpen] = useState(false);

  const {
    rooms,
    page,
    totalPages,
    totalElements,
    loading,
    error: loadError,
    refresh,
  } = useRoomPage({
    userId: user?.id,
    scope: "public",
    page: filters.page,
    size: 10,
    keyword: filters.keyword,
    dramaId: filters.dramaId,
    roomType: filters.roomType,
    sort: filters.sort,
  });

  // 검색 입력이 잠시 멈추면 검색어와 페이지를 함께 변경
  useEffect(() => {
    const timer = window.setTimeout(() => {
      setFilters((current) => {
        if (current.keyword === query) return current;

        return {
          ...current,
          keyword: query,
          page: 0,
        };
      });
    }, 300);

    return () => window.clearTimeout(timer);
  }, [query]);

  // 기존 검색 조건 저장 유지
  useEffect(() => {
    try {
      sessionStorage.setItem(
        "spoilock.chatListFilters",
        JSON.stringify({
          query,
          dramaId: filters.dramaId,
          roomType: filters.roomType,
          sort: filters.sort,
        }),
      );
    } catch {
      // 저장할 수 없는 환경에서도 검색은 사용 가능
    }
  }, [query, filters.dramaId, filters.roomType, filters.sort]);

  useEffect(() => {
    let active = true;

    async function loadDramas() {
      try {
        const data = await dramaApi.getDramas();

        if (!active) return;

        setDramas(data ?? []);
        setDramaError("");
      } catch {
        if (!active) return;

        setDramaError(
          "드라마 선택 목록과 포스터를 불러오지 못했습니다. 채팅방 검색은 계속 사용할 수 있습니다.",
        );
      } finally {
        if (active) setDramasLoading(false);
      }
    }

    void loadDramas();

    return () => {
      active = false;
    };
  }, [dramaRevision]);

  const dramaById = new Map(dramas.map((drama) => [String(drama.id), drama]));

  const dramaOptions = [...dramas].sort((a, b) =>
    a.title.localeCompare(b.title, "ko"),
  );

  const hasFilters =
    query !== "" || filters.dramaId !== "" || filters.roomType !== "ALL";

  const searchPending = query !== filters.keyword;

  function changeFilters(changes) {
    setFilters((current) => ({
      ...current,
      ...changes,
      page: 0,
    }));
  }

  function clearSearch() {
    setQuery("");
    changeFilters({ keyword: "" });
    searchRef.current?.focus();
  }

  function resetFilters() {
    setQuery("");

    setFilters((current) => ({
      ...current,
      keyword: "",
      dramaId: "",
      roomType: "ALL",
      page: 0,
    }));
  }

  function reload() {
    refresh();
    setDramaRevision((current) => current + 1);
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

  function openCreateRoom() {
    if (!user) {
      onLogin();
      return;
    }

    setCreateRoomOpen(true);
  }

  return (
    <main className="container main-content chat-directory">
      <header className="chat-directory-heading">
        <div>
          <span className="section-overline">LIVE ROOMS</span>
          <h1>채팅방</h1>
          <p>좋아하는 드라마를 찾고, 함께 이야기해 보세요.</p>
        </div>

        <Button variant="outline" onClick={openCreateRoom}>
          ＋ 채팅방 만들기
        </Button>
      </header>

      <section className="chat-directory-tools" aria-label="채팅방 검색과 필터">
        <div className="chat-directory-search">
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            aria-hidden="true"
          >
            <circle cx="10.5" cy="10.5" r="6.5" />
            <path d="m16 16 4.5 4.5" />
          </svg>

          <input
            ref={searchRef}
            type="search"
            maxLength={200}
            aria-label="드라마 이름이나 채팅방 이름 검색"
            placeholder="드라마 이름이나 채팅방 이름으로 검색"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />

          {query && (
            <button
              type="button"
              className="chat-directory-clear"
              aria-label="검색어 지우기"
              onClick={clearSearch}
            >
              ×
            </button>
          )}
        </div>

        <div className="chat-directory-filters">
          <select
            aria-label="드라마 선택"
            value={filters.dramaId}
            onChange={(event) => changeFilters({ dramaId: event.target.value })}
            disabled={dramasLoading || Boolean(dramaError)}
          >
            <option value="">전체 드라마</option>

            {filters.dramaId && !dramaById.has(filters.dramaId) && (
              <option value={filters.dramaId}>이전에 선택한 드라마</option>
            )}

            {dramaOptions.map((drama) => (
              <option key={drama.id} value={String(drama.id)}>
                {drama.title}
              </option>
            ))}
          </select>

          <div
            className="chat-directory-types"
            role="group"
            aria-label="채팅방 종류"
          >
            {ROOM_TYPES.map((type) => (
              <button
                key={type.value}
                type="button"
                aria-pressed={filters.roomType === type.value}
                onClick={() => changeFilters({ roomType: type.value })}
              >
                {type.label}
              </button>
            ))}
          </div>

          {hasFilters && (
            <button
              type="button"
              className="chat-directory-reset"
              onClick={resetFilters}
            >
              필터 초기화
            </button>
          )}
        </div>
      </section>

      {dramaError && (
        <div className="chat-directory-warning" role="status">
          <span>{dramaError}</span>
          <button type="button" onClick={reload}>
            다시 불러오기
          </button>
        </div>
      )}

      {loadError && (
        <div className="chat-directory-warning" role="alert">
          <span>{loadError}</span>
          <button type="button" onClick={refresh}>
            다시 불러오기
          </button>
        </div>
      )}

      <div className="chat-directory-summary">
        <p role="status">
          {loading || searchPending
            ? "목록을 불러오는 중"
            : loadError
              ? "목록 갱신에 실패했습니다."
              : `검색 결과 ${totalElements}개${
                  totalPages > 0 ? ` · ${page + 1} / ${totalPages}페이지` : ""
                }`}
        </p>

        <select
          aria-label="채팅방 정렬"
          value={filters.sort}
          onChange={(event) => changeFilters({ sort: event.target.value })}
        >
          <option value="members">가입 인원순</option>
          <option value="name">이름순</option>
        </select>
      </div>

      <section
        className="chat-directory-list"
        aria-label="채팅방 목록"
        aria-busy={loading || searchPending}
      >
        {loading || searchPending ? (
          <div className="chat-directory-empty">
            <p>채팅방을 불러오는 중입니다.</p>
          </div>
        ) : rooms.length === 0 ? (
          !loadError && (
            <div className="chat-directory-empty">
              <strong>
                {hasFilters
                  ? "검색 조건에 맞는 채팅방이 없습니다."
                  : "아직 만들어진 채팅방이 없습니다."}
              </strong>

              <p>
                {hasFilters
                  ? "검색어나 필터를 변경해 보세요."
                  : "첫 번째 채팅방을 만들어 보세요."}
              </p>

              <Button
                variant="outline"
                onClick={hasFilters ? resetFilters : openCreateRoom}
              >
                {hasFilters ? "필터 초기화" : "채팅방 만들기"}
              </Button>
            </div>
          )
        ) : (
          rooms.map((room) => {
            const drama = dramaById.get(String(room.dramaId));
            const title = drama?.title || "드라마 정보 없음";
            const isDefault = room.roomType === "DEFAULT";

            return (
              <button
                key={room.roomId}
                type="button"
                className="chat-directory-card"
                onClick={() => enterRoom(room.roomId)}
                disabled={enteringRoomId !== null}
              >
                <RoomPoster
                  key={`${room.dramaId}:${drama?.posterUrl ?? ""}`}
                  src={drama?.posterUrl}
                  title={drama?.title || room.roomName}
                />

                <span className="chat-directory-copy">
                  <span className="chat-directory-meta">
                    <span
                      className={`chat-directory-badge ${
                        isDefault ? "is-default" : ""
                      }`}
                    >
                      {TYPE_LABELS[room.roomType] || "채팅방"}
                    </span>

                    <span className="chat-directory-drama">{title}</span>
                  </span>

                  <strong>{room.roomName}</strong>

                  {!isDefault && room.description && (
                    <span className="chat-directory-description">
                      {room.description}
                    </span>
                  )}
                </span>

                <span className="chat-directory-count">
                  가입 {room.memberCount ?? 0}명
                </span>

                <span className="chat-directory-enter">
                  {enteringRoomId === room.roomId ? "입장 중…" : "입장 →"}
                </span>
              </button>
            );
          })
        )}
      </section>

      {!loading && !searchPending && (
        <Pagination
          page={page}
          totalPages={totalPages}
          disabled={enteringRoomId !== null}
          onPageChange={(nextPage) =>
            setFilters((current) => ({
              ...current,
              page: nextPage,
            }))
          }
          label="채팅방 목록 페이지"
        />
      )}

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
