import { useEffect, useRef, useState } from "react";
import { dramaApi } from "../../api/dramaApi";

export default function WatchProgressPanel({ dramaId, episode }) {
  const activeRef = useRef(false);
  const savingRef = useRef(false);

  const [watched, setWatched] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    activeRef.current = true;

    return () => {
      activeRef.current = false;
    };
  }, []);

  useEffect(() => {
    let active = true;

    async function load() {
      try {
        const data = await dramaApi.getProgress(dramaId);

        if (!active) return;

        // 영상 제공 작품의 진도는 감소하지 않음
        setWatched((current) =>
          Math.max(current ?? 0, data.watchedEpisodeNumber),
        );
        setError("");
      } catch (err) {
        if (active) {
          setError(err.message || "시청 진도를 불러오지 못했습니다.");
        }
      }
    }

    void load();

    function onFocus() {
      void load();
    }

    window.addEventListener("focus", onFocus);

    return () => {
      active = false;
      window.removeEventListener("focus", onFocus);
    };
  }, [dramaId, revision]);

  async function saveProgress() {
    if (
      savingRef.current ||
      watched == null ||
      !episode ||
      episode.episodeNumber <= watched
    ) {
      return;
    }

    const target = episode.episodeNumber;

    const confirmed = window.confirm(
      `${target}화까지 시청한 것으로 설정할까요?\n\n` +
        `채팅과 커뮤니티에서 ${target}화까지의 내용이 표시될 수 있습니다.\n` +
        "영상 제공 작품은 시청 진도를 다시 낮출 수 없습니다.",
    );

    if (!confirmed) return;

    savingRef.current = true;
    setSaving(true);
    setError("");
    setMessage("");

    try {
      const data = await dramaApi.updateProgress(dramaId, target);

      if (!activeRef.current) return;

      setWatched((current) =>
        Math.max(current ?? 0, data.watchedEpisodeNumber),
      );
      setMessage(
        `${data.watchedEpisodeNumber}화까지 시청 진도를 저장했습니다.`,
      );
    } catch (err) {
      if (!activeRef.current) return;

      setError(err.message || "시청 진도를 저장하지 못했습니다.");

      // 다른 브라우저에서 더 높은 진도가 저장됐을 수 있음
      try {
        const latest = await dramaApi.getProgress(dramaId);

        if (activeRef.current) {
          setWatched((current) =>
            Math.max(current ?? 0, latest.watchedEpisodeNumber),
          );
        }
      } catch {
        // 기존 저장 오류 안내 유지
      }
    } finally {
      savingRef.current = false;

      if (activeRef.current) {
        setSaving(false);
      }
    }
  }

  const alreadyWatched =
    watched != null && episode && episode.episodeNumber <= watched;

  return (
    <section className="watch-progress-panel" aria-label="수동 시청 진도">
      <div className="watch-progress-main">
        <div>
          <span className="watch-kicker">MY PROGRESS</span>
          <h3>
            {watched == null
              ? "시청 진도 확인 중"
              : watched === 0
                ? "아직 설정된 시청 진도가 없어요"
                : `${watched}화까지 시청했어요`}
          </h3>
          <p>시청을 마쳤다면 직접 진도를 설정해 주세요.</p>
        </div>

        <button
          type="button"
          className="watch-primary-button"
          onClick={saveProgress}
          disabled={saving || watched == null || !episode || alreadyWatched}
        >
          {saving
            ? "저장 중…"
            : alreadyWatched
              ? "이미 반영된 회차"
              : episode
                ? `${episode.episodeNumber}화까지 시청했어요`
                : "회차를 선택해 주세요"}
        </button>
      </div>

      <p className="watch-progress-caption">
        영상 재생으로 진도가 자동 변경되지 않으며, 저장한 진도는 낮출 수
        없습니다.
      </p>

      {message && (
        <p className="watch-success" role="status">
          {message}
        </p>
      )}

      {error && (
        <div className="watch-inline-error" role="alert">
          <span>{error}</span>
          <button
            type="button"
            disabled={saving}
            onClick={() => {
              setError("");
              setRevision((value) => value + 1);
            }}
          >
            진도 다시 확인
          </button>
        </div>
      )}
    </section>
  );
}
