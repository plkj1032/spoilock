import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";

import { chatApi } from "../api/chatApi";
import { dramaApi } from "../api/dramaApi";
import Button from "../components/common/Button";
import ChatRoomCreate from "../components/chat/ChatRoomCreate";

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

function normalize(value) {
  return String(value ?? "")
    .normalize("NFKC")
    .toLocaleLowerCase("ko-KR")
    .replace(/\s+/g, "");
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

  const [rooms, setRooms] = useState([]);
  const [dramas, setDramas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [dramaError, setDramaError] = useState("");
  const [revision, setRevision] = useState(0);

  const [initialFilters] = useState(() => {
    try {
      const saved = JSON.parse(
        sessionStorage.getItem("spoilock.chatListFilters") || "{}",
      );

      return {
        query: typeof saved?.query === "string" ? saved.query : "",
        dramaId: typeof saved?.dramaId === "string" ? saved.dramaId : "",
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
  });

  const [query, setQuery] = useState(initialFilters.query);
  const [dramaId, setDramaId] = useState(initialFilters.dramaId);
  const [roomType, setRoomType] = useState(initialFilters.roomType);
  const [sort, setSort] = useState(initialFilters.sort);
  useEffect(() => {
    try {
      sessionStorage.setItem(
        "spoilock.chatListFilters",
        JSON.stringify({
          query,
          dramaId,
          roomType,
          sort,
        }),
      );
    } catch {
      // 브라우저에서 저장을 허용하지 않아도 검색은 그대로 사용
    }
  }, [query, dramaId, roomType, sort]);
  const [enteringRoomId, setEnteringRoomId] = useState(null);
  const [createRoomOpen, setCreateRoomOpen] = useState(false);

  useEffect(() => {
    let active = true;

    Promise.allSettled([chatApi.getRooms(), dramaApi.getDramas()]).then(
      ([roomResult, dramaResult]) => {
        if (!active) return;

        if (roomResult.status === "fulfilled") {
          setRooms(roomResult.value ?? []);
          setLoadError("");
        } else {
          setRooms([]);
          setLoadError("채팅방 목록을 불러오지 못했습니다.");
        }

        if (dramaResult.status === "fulfilled") {
          setDramas(dramaResult.value ?? []);
          setDramaError("");
        } else {
          setDramas([]);
          setDramaError(
            "드라마 정보를 불러오지 못했습니다. 지금은 방 이름으로 검색할 수 있습니다.",
          );
        }

        setLoading(false);
      },
    );

    return () => {
      active = false;
    };
  }, [revision]);

  const dramaById = new Map(dramas.map((drama) => [String(drama.id), drama]));

  const dramaOptions = [...dramas].sort((a, b) =>
    a.title.localeCompare(b.title, "ko"),
  );

  const searchText = normalize(query);

  const visibleRooms = rooms
    .filter((room) => {
      const drama = dramaById.get(String(room.dramaId));

      const matchesSearch =
        !searchText ||
        normalize(room.roomName).includes(searchText) ||
        normalize(drama?.title).includes(searchText);

      const matchesDrama = !dramaId || String(room.dramaId) === dramaId;

      const matchesType = roomType === "ALL" || room.roomType === roomType;

      return matchesSearch && matchesDrama && matchesType;
    })
    .sort((a, b) => {
      if (sort === "members") {
        const difference =
          Number(b.memberCount ?? 0) - Number(a.memberCount ?? 0);

        if (difference !== 0) return difference;
      }

      return (
        a.roomName.localeCompare(b.roomName, "ko") ||
        Number(a.roomId) - Number(b.roomId)
      );
    });

  const hasFilters = query !== "" || dramaId !== "" || roomType !== "ALL";

  function resetFilters() {
    setQuery("");
    setDramaId("");
    setRoomType("ALL");
  }

  function reload() {
    setLoading(true);
    setLoadError("");
    setDramaError("");
    setRevision((current) => current + 1);
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
              onClick={() => {
                setQuery("");
                searchRef.current?.focus();
              }}
            >
              ×
            </button>
          )}
        </div>

        <div className="chat-directory-filters">
          <select
            aria-label="드라마 선택"
            value={dramaId}
            onChange={(event) => setDramaId(event.target.value)}
            disabled={loading || Boolean(dramaError)}
          >
            <option value="">전체 드라마</option>

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
                aria-pressed={roomType === type.value}
                onClick={() => setRoomType(type.value)}
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

      {!loading && dramaError && !loadError && (
        <div className="chat-directory-warning" role="status">
          <span>{dramaError}</span>
          <button type="button" onClick={reload}>
            다시 불러오기
          </button>
        </div>
      )}

      <div className="chat-directory-summary">
        <p role="status">
          {loading
            ? "목록을 불러오는 중"
            : loadError
              ? "목록 조회 실패"
              : `검색 결과 ${visibleRooms.length}개`}
        </p>

        <select
          aria-label="채팅방 정렬"
          value={sort}
          onChange={(event) => setSort(event.target.value)}
        >
          <option value="members">가입 인원순</option>
          <option value="name">이름순</option>
        </select>
      </div>

      <section
        className="chat-directory-list"
        aria-label="채팅방 목록"
        aria-busy={loading}
      >
        {loading ? (
          <div className="chat-directory-empty">
            <p>채팅방을 불러오는 중입니다.</p>
          </div>
        ) : loadError ? (
          <div className="chat-directory-empty" role="alert">
            <strong>{loadError}</strong>
            <Button variant="outline" onClick={reload}>
              다시 불러오기
            </Button>
          </div>
        ) : visibleRooms.length === 0 ? (
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
        ) : (
          visibleRooms.map((room) => {
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

      {createRoomOpen && (
        <ChatRoomCreate onClose={() => setCreateRoomOpen(false)} />
      )}
    </main>
  );
}
