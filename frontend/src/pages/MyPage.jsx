import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";

import { myPageApi } from "../api/myPageApi";
import MyDramaActions from "../components/drama/MyDramaActions";
import "../styles/MyPage.css";
import Pagination from "../components/common/Pagination";
import useRoomPage from "../hooks/useRoomPage";
import MySocialStats from "../components/user/MySocialStats";
import ProfileEditModal from "../components/user/ProfileEditModal";
import DefaultRoomCrown from "../components/common/DefaultRoomCrown";
import MyRoomFilters from "../components/common/MyRoomFilters";

function getWatchStatus(drama) {
  const watched = Number(drama.watchedEpisodeNumber ?? 0);
  const total = Number(drama.totalEpisodeCount ?? 0);

  if (total > 0 && watched >= total) return "완주";
  if (watched > 0) return "시청 중";
  return "시청 전";
}

function DramaPoster({ src, title }) {
  const [failed, setFailed] = useState(false);

  return (
    <div className="mypage-poster">
      {src && !failed ? (
        <img
          src={src}
          alt={`${title} 포스터`}
          loading="lazy"
          onError={() => setFailed(true)}
        />
      ) : (
        <span aria-hidden="true">{Array.from(title || "?")[0]}</span>
      )}
    </div>
  );
}

export default function MyPage({ onProfileUpdated }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [revision, setRevision] = useState(0);

  const [nickname, setNickname] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");
  const [saveMessage, setSaveMessage] = useState("");
  const [watchFilter, setWatchFilter] = useState("전체");
  const [requestedRoomPage, setRequestedRoomPage] = useState(0);
  const [profileEditOpen, setProfileEditOpen] = useState(false);
  const [roomDramaId, setRoomDramaId] = useState("");
  const [roomTypeFilter, setRoomTypeFilter] = useState("ALL");
  const {
    rooms,
    page: roomPage,
    totalPages: roomTotalPages,
    totalElements: roomTotalElements,
    loading: roomsLoading,
    error: roomsError,
    refresh: refreshRooms,
  } = useRoomPage({
    userId: data?.profile?.id,
    scope: "mine",
    page: requestedRoomPage,
    size: 5,
    dramaId: roomDramaId,
    roomType: roomTypeFilter,
  });
  const savingRef = useRef(false);
  const mountedRef = useRef(false);
  const saveControllerRef = useRef(null);

  useEffect(() => {
    mountedRef.current = true;

    return () => {
      mountedRef.current = false;
      saveControllerRef.current?.abort();
    };
  }, []);

  useEffect(() => {
    let active = true;
    const controller = new AbortController();

    async function load() {
      try {
        const result = await myPageApi.getMyPage(controller.signal);

        if (!active) return;

        setData(result);
        setNickname(result.profile.nickname);
        setLoadError("");
      } catch (error) {
        if (!active || error.name === "AbortError") return;
        setLoadError(error.message);
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();

    return () => {
      active = false;
      controller.abort();
    };
  }, [revision]);

  async function saveProfile(event) {
    event.preventDefault();

    if (savingRef.current || !data) return;

    const normalized = nickname.trim();

    if (normalized.length < 2 || normalized.length > 20) {
      setSaveError("닉네임은 앞뒤 공백을 제외하고 2~20자로 입력해 주세요.");
      setSaveMessage("");
      return;
    }

    if (normalized === data.profile.nickname) {
      setSaveMessage("변경된 내용이 없습니다.");
      setSaveError("");
      return;
    }

    savingRef.current = true;
    setSaving(true);
    setSaveError("");
    setSaveMessage("");

    const controller = new AbortController();
    saveControllerRef.current = controller;

    try {
      const profile = await myPageApi.updateProfile(
        normalized,
        controller.signal,
      );

      if (!mountedRef.current) return;

      setData((current) => ({ ...current, profile }));
      setNickname(profile.nickname);
      onProfileUpdated(profile);
      setSaveMessage("닉네임을 변경했습니다.");
    } catch (error) {
      if (!mountedRef.current || error.name === "AbortError") return;
      setSaveError(error.message);
    } finally {
      savingRef.current = false;

      if (mountedRef.current) {
        setSaving(false);
      }
    }
  }
  function handleProgressSaved(dramaId, watchedEpisodeNumber) {
    setData((current) => {
      if (!current) return current;

      return {
        ...current,
        dramas: current.dramas.map((drama) =>
          String(drama.dramaId) === String(dramaId)
            ? { ...drama, watchedEpisodeNumber }
            : drama,
        ),
      };
    });
  }
  if (loading) {
    return (
      <main className="container main-content mypage">
        <p className="mypage-empty" role="status">
          내 정보를 불러오는 중입니다.
        </p>
      </main>
    );
  }

  if (loadError || !data) {
    return (
      <main className="container main-content mypage">
        <section className="mypage-panel">
          <p role="alert">{loadError || "내 정보를 불러오지 못했습니다."}</p>
          <button
            type="button"
            className="mypage-button"
            onClick={() => {
              setLoading(true);
              setLoadError("");
              setRevision((current) => current + 1);
            }}
          >
            다시 불러오기
          </button>
        </section>
      </main>
    );
  }

  const { profile, dramas } = data;

  const watchingCount = dramas.filter(
    (drama) => getWatchStatus(drama) === "시청 중",
  ).length;

  const completedCount = dramas.filter(
    (drama) => getWatchStatus(drama) === "완주",
  ).length;

  const visibleDramas = dramas.filter(
    (drama) => watchFilter === "전체" || getWatchStatus(drama) === watchFilter,
  );

  return (
    <main className="container main-content mypage">
      <header className="mypage-heading">
        <span className="section-overline">MY SPOILOCK</span>
        <h1>마이페이지</h1>
        <p>나의 시청 기록과 함께 이야기하는 공간을 모아봤어요.</p>
      </header>

      <section className="mypage-profile mypage-panel">
        <div className="mypage-identity">
          <div className="mypage-avatar" aria-hidden="true">
            {Array.from(profile.nickname || "?")[0]}
          </div>
          <div>
            <div className="mypage-profile-name-row">
              <h2>{profile.nickname}님</h2>

              <button
                type="button"
                className="mypage-profile-edit-button"
                aria-haspopup="dialog"
                onClick={() => {
                  setNickname(profile.nickname);
                  setSaveError("");
                  setSaveMessage("");
                  setProfileEditOpen(true);
                }}
              >
                <svg
                  viewBox="0 0 24 24"
                  width="13"
                  height="13"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <path d="m16 3 5 5-12 12-6 1 1-6L16 3Z" />
                  <path d="m13 6 5 5" />
                </svg>
                수정
              </button>
            </div>

            <p>{profile.email || "등록된 이메일 없음"}</p>
          </div>
        </div>

        <MySocialStats key={profile.id} currentUser={profile} />
      </section>
      <section className="mypage-panel" aria-labelledby="mypage-dramas">
        <div className="mypage-section-heading">
          <div>
            <h2 id="mypage-dramas">내 시청 목록</h2>

            <div className="mypage-section-counts">
              <span>
                시청 중 <strong>{watchingCount}</strong>
              </span>
              <span>
                완주 <strong>{completedCount}</strong>
              </span>
            </div>
          </div>
          <Link className="mypage-link" to="/dramas">
            드라마 둘러보기 →
          </Link>
        </div>

        <div className="mypage-tabs" role="group" aria-label="시청 상태">
          {["전체", "시청 중", "완주"].map((filter) => (
            <button
              key={filter}
              type="button"
              aria-pressed={watchFilter === filter}
              onClick={() => setWatchFilter(filter)}
            >
              {filter}
            </button>
          ))}
        </div>

        {visibleDramas.length === 0 ? (
          <p className="mypage-empty">
            {dramas.length === 0
              ? "아직 등록한 시청 진도가 없어요. 드라마에서 시청 진도를 설정해 주세요."
              : "해당 상태의 드라마가 없습니다."}
          </p>
        ) : (
          <div className="mypage-drama-grid">
            {visibleDramas.map((drama) => {
              const watched = Number(drama.watchedEpisodeNumber ?? 0);
              const total = Number(drama.totalEpisodeCount ?? 0);
              const percent =
                total > 0 ? Math.min(100, (watched / total) * 100) : 0;

              return (
                <article className="mypage-drama-card" key={drama.dramaId}>
                  <DramaPoster
                    key={`${drama.dramaId}:${drama.posterUrl}`}
                    src={drama.posterUrl}
                    title={drama.title}
                  />

                  <div className="mypage-drama-copy">
                    <span className="mypage-badge">
                      {getWatchStatus(drama)}
                    </span>

                    <h3>{drama.title}</h3>

                    <p>
                      {watched}화 시청
                      {total > 0 && ` · 총 ${total}화`}
                    </p>

                    <progress
                      value={percent}
                      max="100"
                      aria-label={`${drama.title} 시청 진도`}
                    />

                    <MyDramaActions
                      drama={drama}
                      onProgressSaved={handleProgressSaved}
                    />
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>

      <section className="mypage-panel" aria-labelledby="mypage-rooms">
        <div className="mypage-section-heading">
          <div>
            <h2 id="mypage-rooms">참여 중인 채팅방</h2>

            <div className="mypage-section-counts">
              <span>
                {roomDramaId !== "" || roomTypeFilter !== "ALL"
                  ? "필터 결과"
                  : "전체"}
                <strong>
                  {roomsLoading ? "…" : roomsError ? "—" : roomTotalElements}
                </strong>
              </span>
            </div>
          </div>
          <Link className="mypage-link" to="/chat">
            채팅방 둘러보기 →
          </Link>
        </div>
        <MyRoomFilters
          dramaId={roomDramaId}
          roomType={roomTypeFilter}
          onDramaChange={(value) => {
            setRoomDramaId(value);
            setRequestedRoomPage(0);
          }}
          onTypeChange={(value) => {
            setRoomTypeFilter(value);
            setRequestedRoomPage(0);
          }}
        />
        {roomsError && (
          <div role="alert">
            <p className="mypage-error">{roomsError}</p>
            <button
              type="button"
              className="mypage-button"
              onClick={refreshRooms}
            >
              다시 불러오기
            </button>
          </div>
        )}

        <div aria-busy={roomsLoading}>
          {roomsLoading ? (
            <p className="mypage-empty" role="status">
              참여 중인 채팅방을 불러오는 중입니다.
            </p>
          ) : rooms.length === 0 ? (
            !roomsError && (
              <p className="mypage-empty">
                {roomDramaId !== "" || roomTypeFilter !== "ALL"
                  ? "선택한 조건에 해당하는 참여 채팅방이 없습니다."
                  : "아직 참여 중인 채팅방이 없습니다."}
              </p>
            )
          ) : (
            <div className="mypage-room-list">
              {rooms.map((room) => (
                <Link
                  key={room.roomId}
                  to={`/chat/${room.roomId}`}
                  className={`mypage-room ${
                    room.roomType === "DEFAULT" ? "is-default-room" : ""
                  }`}
                >
                  <div className="mypage-default-room-heading">
                    {room.roomType === "DEFAULT" && <DefaultRoomCrown />}

                    <div>
                      <span className="mypage-room-drama">
                        {room.dramaTitle}
                      </span>

                      <h3>{room.roomName}</h3>

                      <span className="mypage-badge">
                        {room.roomType === "DEFAULT"
                          ? "토론방"
                          : room.roomType === "PRIVATE"
                            ? "비공개방"
                            : "자유 채팅방"}
                      </span>
                    </div>
                  </div>

                  <span className="mypage-room-enter">입장 →</span>
                </Link>
              ))}
            </div>
          )}
        </div>

        {!roomsLoading && roomTotalElements > 0 && (
          <>
            <Pagination
              page={roomPage}
              totalPages={roomTotalPages}
              onPageChange={setRequestedRoomPage}
              label="내 참여 채팅방 페이지"
            />
          </>
        )}
      </section>

      {profileEditOpen && (
        <ProfileEditModal
          email={profile.email}
          nickname={nickname}
          saving={saving}
          error={saveError}
          message={saveMessage}
          onNicknameChange={(value) => {
            setNickname(value);
            setSaveError("");
            setSaveMessage("");
          }}
          onSubmit={saveProfile}
          onClose={() => {
            if (savingRef.current) return;
            setProfileEditOpen(false);
          }}
        />
      )}
    </main>
  );
}
