import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { dramaApi } from "../api/dramaApi";
import { dramaWatchApi } from "../api/dramaWatchApi";

import "../styles/DramaWatchPage.css";

function EpisodePlayer({ dramaId, episode }) {
  const savedTimeRef = useRef(0);

  const [playback, setPlayback] = useState(null);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    dramaWatchApi
      .getPlayback(dramaId, episode.episodeId, controller.signal)
      .then((data) => {
        if (active) setPlayback(data);
      })
      .catch((err) => {
        if (active && !controller.signal.aborted) {
          setError(err.message || "영상 재생 정보를 불러오지 못했습니다.");
        }
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [dramaId, episode.episodeId, revision]);

  function retry() {
    setPlayback(null);
    setError("");
    setRevision((value) => value + 1);
  }

  return (
    <section className="watch-player-panel" aria-label="영상 플레이어">
      {error ? (
        <div className="watch-player-placeholder">
          <p role="alert">{error}</p>
          <button type="button" onClick={retry}>
            재생 다시 시도
          </button>
        </div>
      ) : !playback ? (
        <div className="watch-player-placeholder" role="status">
          영상을 준비하고 있어요…
        </div>
      ) : (
        <video
          key={playback.playbackUrl}
          className="watch-video"
          controls
          playsInline
          preload="metadata"
          src={playback.playbackUrl}
          aria-label={`${episode.episodeNumber}화 ${episode.title}`}
          onTimeUpdate={(event) => {
            const time = event.currentTarget.currentTime;

            if (Number.isFinite(time)) {
              savedTimeRef.current = time;
            }
          }}
          onLoadedMetadata={(event) => {
            const video = event.currentTarget;

            if (savedTimeRef.current > 0 && Number.isFinite(video.duration)) {
              video.currentTime = Math.min(
                savedTimeRef.current,
                Math.max(0, video.duration - 0.1),
              );
            }
          }}
          onError={() => {
            setError(
              "영상을 재생하지 못했습니다. 재생 주소 만료 또는 영상 형식 문제일 수 있습니다.",
            );
          }}
        >
          브라우저가 영상 재생을 지원하지 않습니다.
        </video>
      )}

      <div className="watch-current-episode">
        <span>현재 선택한 회차</span>
        <h2>
          {episode.episodeNumber}화 · {episode.title}
        </h2>
      </div>
    </section>
  );
}

function WatchContent({ dramaId }) {
  const [drama, setDrama] = useState(null);
  const [episodes, setEpisodes] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    async function load() {
      try {
        const [detail, items] = await Promise.all([
          dramaApi.getDetail(dramaId, controller.signal),
          dramaWatchApi.getEpisodes(dramaId, controller.signal),
        ]);

        if (!active) return;

        const sorted = [...items].sort(
          (a, b) => a.episodeNumber - b.episodeNumber,
        );

        setDrama(detail);
        setEpisodes(sorted);
        setSelectedId(sorted.find((item) => item.playable)?.episodeId ?? null);
      } catch (err) {
        if (active && !controller.signal.aborted) {
          setError(
            err.message || "시청할 수 있는 작품 정보를 불러오지 못했습니다.",
          );
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();

    return () => {
      active = false;
      controller.abort();
    };
  }, [dramaId, revision]);

  function reload() {
    setError("");
    setLoading(true);
    setRevision((value) => value + 1);
  }

  const selectedEpisode = episodes.find(
    (episode) => episode.episodeId === selectedId,
  );

  if (loading) {
    return (
      <main className="container main-content watch-page">
        <p role="status">작품을 불러오는 중…</p>
      </main>
    );
  }

  if (error) {
    return (
      <main className="container main-content watch-page">
        <Link to="/dramas" className="watch-back">
          ← 드라마 목록
        </Link>

        <div className="watch-empty">
          <p role="alert">{error}</p>
          <button type="button" onClick={reload}>
            다시 시도
          </button>
        </div>
      </main>
    );
  }

  return (
    <main className="container main-content watch-page">
      <Link to="/dramas" className="watch-back">
        ← 드라마 목록
      </Link>

      <header className="watch-heading">
        <div>
          <span>SPOILOCK ORIGINAL</span>
          <h1>{drama.title}</h1>
          <p>총 {drama.totalEpisodeCount}부작</p>
        </div>

        <div className="watch-links">
          <Link to={`/chat?dramaId=${dramaId}`}>채팅방</Link>
          <Link to={`/community?dramaId=${dramaId}`}>커뮤니티</Link>
        </div>
      </header>

      <div className="watch-layout">
        <div>
          {selectedEpisode ? (
            <EpisodePlayer
              key={selectedEpisode.episodeId}
              dramaId={dramaId}
              episode={selectedEpisode}
            />
          ) : (
            <div className="watch-empty">
              <strong>영상 준비 중</strong>
              <p>아직 재생할 수 있는 회차가 없습니다.</p>
              <button type="button" onClick={reload}>
                다시 확인
              </button>
            </div>
          )}

          <p className="watch-progress-note">
            영상을 재생해도 시청 진도는 자동으로 변경되지 않습니다. 기존 시청
            진도 설정에서 직접 변경해 주세요. 영상 제공 작품은 설정한 진도를
            낮출 수 없습니다.
          </p>
        </div>

        <aside className="watch-episodes" aria-label="회차 선택">
          <div className="watch-episodes-heading">
            <h2>회차 목록</h2>
            <span>{episodes.length}개</span>
          </div>

          <div className="watch-episode-list">
            {episodes.map((episode) => {
              const selected = episode.episodeId === selectedId;

              return (
                <button
                  key={episode.episodeId}
                  type="button"
                  className={`watch-episode ${selected ? "is-selected" : ""}`}
                  disabled={!episode.playable}
                  aria-pressed={selected}
                  onClick={() => setSelectedId(episode.episodeId)}
                >
                  <span className="watch-episode-number">
                    {episode.episodeNumber}화
                  </span>

                  <span className="watch-episode-copy">
                    <strong>{episode.title}</strong>
                    <small>
                      {!episode.playable
                        ? "준비 중"
                        : selected
                          ? "선택한 회차"
                          : "시청하기"}
                    </small>
                  </span>

                  <span aria-hidden="true">{episode.playable ? "▶" : "—"}</span>
                </button>
              );
            })}
          </div>
        </aside>
      </div>
    </main>
  );
}

export default function DramaWatchPage() {
  const { dramaId } = useParams();

  return <WatchContent key={dramaId} dramaId={dramaId} />;
}
