import { useEffect, useState } from "react";
import { dramaApi } from "../../api/dramaApi";
import DramaCard from "./DramaCard";

export default function DramaGallery({ user, onLogin, onNotice, limit }) {
  const [dramas, setDramas] = useState([]);
  const [progressById, setProgressById] = useState({});
  const [loading, setLoading] = useState(true);
  const [progressLoaded, setProgressLoaded] = useState(false);

  const [selectedDrama, setSelectedDrama] = useState(null);
  const [episodes, setEpisodes] = useState([]);
  const [current, setCurrent] = useState(0);
  const [selected, setSelected] = useState(0);

  const [dialogLoading, setDialogLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [dialogError, setDialogError] = useState("");

  const progressLoading =
    Boolean(user?.id) && dramas.length > 0 && !progressLoaded;

  // 1. 공개 드라마 목록 조회
  useEffect(() => {
    let active = true;

    dramaApi
      .getDramas()
      .then((data) => {
        if (active) setDramas(data);
      })
      .catch((error) => {
        if (active) onNotice(error.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [onNotice]);

  // 2. 로그인 사용자별 시청 진도 조회

  useEffect(() => {
    if (!user?.id || dramas.length === 0) {
      return;
    }

    let active = true;

    Promise.allSettled(
      dramas.map((drama) => dramaApi.getProgress(drama.id)),
    ).then((results) => {
      if (!active) return;

      const next = {};

      results.forEach((result, index) => {
        if (result.status === "fulfilled") {
          next[dramas[index].id] = result.value.watchedEpisodeNumber;
        }
      });

      setProgressById(next);
      setProgressLoaded(true);

      if (results.some((r) => r.status === "rejected")) {
        onNotice("일부 시청 진도를 불러오지 못했습니다.");
      }
    });

    return () => {
      active = false;
    };
  }, [user?.id, dramas, onNotice]);
  // 3. 시청 진도 설정창 열기
  async function openProgress(drama) {
    if (!user) {
      onLogin();
      return;
    }

    setSelectedDrama(drama);
    setDialogLoading(true);
    setDialogError("");
    setEpisodes([]);

    try {
      const [episodeData, progressData] = await Promise.all([
        dramaApi.getEpisodes(drama.id),
        dramaApi.getProgress(drama.id),
      ]);

      setEpisodes(episodeData);
      setCurrent(progressData.watchedEpisodeNumber);
      setSelected(progressData.watchedEpisodeNumber);

      setProgressById((prev) => ({
        ...prev,
        [drama.id]: progressData.watchedEpisodeNumber,
      }));
    } catch (error) {
      setDialogError(error.message);
    } finally {
      setDialogLoading(false);
    }
  }

  function closeProgress() {
    if (!saving) setSelectedDrama(null);
  }

  // 4. 시청 진도 저장
  async function saveProgress() {
    if (!selectedDrama || saving || dialogLoading) return;

    if (selected === current) {
      closeProgress();
      return;
    }

    // 진도를 올릴 때만 경고
    if (selected > current && !(current === 0 && selected === 1)) {
      const confirmed = window.confirm(
        "⚠️ 스포일러 노출 주의\n\n" +
          `${current}화에서 ${selected}화로 변경하시겠습니까?\n\n` +
          `${selected}화까지의 스포일러가 담긴 ` +
          "메시지나 게시글이 표시될 수 있습니다.\n\n" +
          "실제로 시청한 회차인지 확인해 주세요.",
      );

      if (!confirmed) return;
    }

    setSaving(true);
    setDialogError("");

    try {
      const result = await dramaApi.updateProgress(selectedDrama.id, selected);

      setProgressById((prev) => ({
        ...prev,
        [selectedDrama.id]: result.watchedEpisodeNumber,
      }));

      onNotice(
        `${selectedDrama.title}: ` +
          `${result.watchedEpisodeNumber}화로 시청 진도가 변경되었습니다.`,
      );

      setSelectedDrama(null);
    } catch (error) {
      setDialogError(error.message);
    } finally {
      setSaving(false);
    }
  }

  const visibleDramas = limit ? dramas.slice(0, limit) : dramas;

  if (loading) {
    return <p className="page-subtitle">드라마를 불러오는 중...</p>;
  }

  return (
    <>
      {visibleDramas.length === 0 ? (
        <p className="page-subtitle">현재 공개된 드라마가 없습니다.</p>
      ) : (
        <div className={`drama-grid ${limit ? "" : "all-dramas"}`}>
          {visibleDramas.map((drama) => (
            <DramaCard
              key={drama.id}
              drama={drama}
              watched={progressById[drama.id]}
              loggedIn={!!user}
              progressLoading={progressLoading}
              onAction={() => openProgress(drama)}
            />
          ))}
        </div>
      )}

      {selectedDrama && (
        <div className="modal-backdrop">
          <section
            className="auth-dialog progress-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="progress-dialog-title"
          >
            <button
              type="button"
              className="modal-close"
              onClick={closeProgress}
              disabled={saving}
              aria-label="닫기"
            >
              ×
            </button>

            <h2 id="progress-dialog-title">시청 진도 설정</h2>

            <p className="dialog-description">{selectedDrama.title}</p>

            {dialogLoading ? (
              <p>시청 정보를 불러오는 중...</p>
            ) : (
              <>
                {!dialogError && (
                  <p className="progress-current">현재 {current}화까지 시청</p>
                )}

                <p className="progress-hint">
                  실제로 시청한 마지막 회차를 선택해 주세요.
                </p>

                <div className="progress-episodes">
                  <button
                    type="button"
                    className={selected === 0 ? "active" : ""}
                    onClick={() => setSelected(0)}
                    disabled={saving}
                  >
                    미시청
                  </button>

                  {episodes.map((episode) => (
                    <button
                      key={episode.id}
                      type="button"
                      className={
                        selected === episode.episodeNumber ? "active" : ""
                      }
                      onClick={() => setSelected(episode.episodeNumber)}
                      disabled={saving}
                    >
                      {episode.episodeNumber}화
                    </button>
                  ))}
                </div>

                {dialogError && (
                  <p className="form-error" role="alert">
                    {dialogError}
                  </p>
                )}

                <p className="progress-hint">
                  진도를 올리면 해당 회차의 스포일러가 표시될 수 있습니다.
                </p>

                <button
                  type="button"
                  className="button button-primary progress-save"
                  onClick={saveProgress}
                  disabled={
                    saving ||
                    episodes.length === 0 ||
                    !(
                      selected === 0 ||
                      episodes.some((e) => e.episodeNumber === selected)
                    )
                  }
                >
                  {saving ? "저장 중..." : "시청 진도 저장"}
                </button>
              </>
            )}
          </section>
        </div>
      )}
    </>
  );
}
