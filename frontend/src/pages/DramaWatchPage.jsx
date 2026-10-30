import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { dramaApi } from "../api/dramaApi";
import { dramaWatchApi } from "../api/dramaWatchApi";
import EpisodePlayer from "../components/drama/EpisodePlayer";
import "../styles/DramaWatchPage.css";
import "../styles/DramaWatchLayout.css";
import "../styles/DramaWatchPage.css";
import WatchProgressPanel from "../components/drama/WatchProgressPanel";

function WatchContent({ dramaId }) {
  const [drama, setDrama] = useState(null);
  const [episodes, setEpisodes] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
  const [episodesOpen, setEpisodesOpen] = useState(true);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    async function load() {
      try {
        const [detail, items, resume] = await Promise.all([
          dramaApi.getDetail(dramaId, controller.signal),
          dramaWatchApi.getEpisodes(dramaId, controller.signal),
          dramaWatchApi.getResume(dramaId, controller.signal),
        ]);

        if (!active) return;

        const sorted = [...items].sort(
          (a, b) => a.episodeNumber - b.episodeNumber,
        );

        const lastWatchedEpisode = resume?.saved
          ? sorted.find(
              (item) => item.episodeId === resume.episodeId && item.playable,
            )
          : null;

        const initialEpisode =
          lastWatchedEpisode ?? sorted.find((item) => item.playable) ?? null;

        setDrama(detail);
        setEpisodes(sorted);
        setSelectedId(initialEpisode?.episodeId ?? null);
      } catch (err) {
        if (active && !controller.signal.aborted) {
          setError(err.message || "작품 정보를 불러오지 못했습니다.");
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

  const selectedIndex = episodes.findIndex(
    (episode) => episode.episodeId === selectedId,
  );

  const selectedEpisode = episodes[selectedIndex] ?? null;
  const previousEpisode =
    selectedIndex > 0 ? episodes[selectedIndex - 1] : null;
  const nextEpisode =
    selectedIndex >= 0 ? (episodes[selectedIndex + 1] ?? null) : null;

  function selectEpisode(episode) {
    if (!episode?.playable) return;
    setSelectedId(episode.episodeId);
  }

  if (loading || error) {
    return (
      <main className="container main-content watch-page">
        <Link to="/dramas" className="watch-back">
          ← 드라마 둘러보기
        </Link>

        <div className="watch-empty">
          {loading ? (
            <p role="status">상영관을 준비하고 있어요…</p>
          ) : (
            <>
              <p role="alert">{error}</p>
              <button type="button" onClick={reload}>
                다시 시도
              </button>
            </>
          )}
        </div>
      </main>
    );
  }

  return (
    <main className="container main-content watch-page">
      <div className="watch-topbar">
        <Link to="/dramas" className="watch-back">
          ← 드라마 둘러보기
        </Link>
        <span className="watch-brand">SPOILOCK CINEMA</span>
      </div>

      <header className="watch-heading">
        <div>
          <span className="watch-kicker">NOW WATCHING</span>
          <h1>{drama.title}</h1>

          <div className="watch-meta">
            <span>총 {drama.totalEpisodeCount}부작</span>
            {drama.genres?.map((genre) => (
              <span key={genre.code}>{genre.label}</span>
            ))}
          </div>
        </div>

        <button
          type="button"
          className="watch-list-toggle"
          aria-expanded={episodesOpen}
          aria-controls="watch-episode-panel"
          onClick={() => setEpisodesOpen((open) => !open)}
        >
          {episodesOpen ? "회차 목록 접기" : "회차 목록 열기"}
          <span aria-hidden="true">{episodesOpen ? "−" : "+"}</span>
        </button>
      </header>

      <div className="watch-layout">
        <div className="watch-main-column">
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

          <aside
            id="watch-episode-panel"
            className="watch-episodes"
            aria-label="회차 선택"
            hidden={!episodesOpen}
          >
            <div className="watch-episodes-heading">
              <div>
                <span className="watch-kicker">EPISODES</span>
                <h2>회차 목록</h2>
              </div>
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
                    onClick={() => selectEpisode(episode)}
                  >
                    <span className="watch-episode-number">
                      {String(episode.episodeNumber).padStart(2, "0")}
                    </span>

                    <span className="watch-episode-copy">
                      <strong>{episode.title}</strong>
                      <small>
                        {!episode.playable
                          ? "영상 준비 중"
                          : selected
                            ? "현재 선택한 회차"
                            : `${episode.episodeNumber}화 시청하기`}
                      </small>
                    </span>

                    <span className="watch-episode-icon" aria-hidden="true">
                      {episode.playable ? "▶" : "—"}
                    </span>
                  </button>
                );
              })}
            </div>

            <p className="watch-sidebar-note">
              이전 회차는 언제든 다시 볼 수 있어요.
            </p>
          </aside>

          {selectedEpisode && (
            <nav className="watch-navigation" aria-label="회차 이동">
              <button
                type="button"
                disabled={!previousEpisode?.playable}
                onClick={() => selectEpisode(previousEpisode)}
              >
                <span aria-hidden="true">←</span>
                <span>
                  <small>PREVIOUS</small>
                  <strong>
                    {!previousEpisode
                      ? "첫 번째 회차"
                      : previousEpisode.playable
                        ? `${previousEpisode.episodeNumber}화 보기`
                        : "이전 화 준비 중"}
                  </strong>
                </span>
              </button>

              <button
                type="button"
                disabled={!nextEpisode?.playable}
                onClick={() => selectEpisode(nextEpisode)}
              >
                <span>
                  <small>NEXT</small>
                  <strong>
                    {!nextEpisode
                      ? "마지막 회차"
                      : nextEpisode.playable
                        ? `${nextEpisode.episodeNumber}화 보기`
                        : "다음 화 준비 중"}
                  </strong>
                </span>
                <span aria-hidden="true">→</span>
              </button>
            </nav>
          )}

          <WatchProgressPanel dramaId={dramaId} episode={selectedEpisode} />

          <section className="watch-about">
            <span className="watch-kicker">ABOUT THE SERIES</span>
            <h2>작품 소개</h2>
            <p>{drama.synopsis || "등록된 작품 소개가 없습니다."}</p>

            <dl>
              <div>
                <dt>감독</dt>
                <dd>{drama.directors || "미등록"}</dd>
              </div>
              <div>
                <dt>작가</dt>
                <dd>{drama.writers || "미등록"}</dd>
              </div>
              <div>
                <dt>출연</dt>
                <dd>{drama.castNames || "미등록"}</dd>
              </div>
            </dl>
          </section>
        </div>
      </div>
    </main>
  );
}

export default function DramaWatchPage() {
  const { dramaId } = useParams();

  return <WatchContent key={dramaId} dramaId={dramaId} />;
}
