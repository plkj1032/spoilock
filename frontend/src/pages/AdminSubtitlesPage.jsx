import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";

import { adminDramaApi } from "../api/adminDramaApi";
import { adminSubtitleApi } from "../api/adminSubtitleApi";

export default function AdminSubtitlesPage() {
  const { dramaId } = useParams();

  const [drama, setDrama] = useState(null);
  const [episodeId, setEpisodeId] = useState(null);

  const [analysis, setAnalysis] = useState(null);
  const [events, setEvents] = useState([]);

  const [file, setFile] = useState(null);
  const [manualSummary, setManualSummary] = useState("");

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    let active = true;

    adminDramaApi
      .getDetail(dramaId)
      .then((data) => {
        if (!active) return;

        setDrama(data);
        setEpisodeId(data.episodes?.[0]?.id ?? null);
      })
      .catch((err) => {
        if (active) setError(err.message);
      });

    return () => {
      active = false;
    };
  }, [dramaId]);

  useEffect(() => {
    if (!episodeId) return;

    let active = true;

    Promise.all([
      adminSubtitleApi.getAnalysis(episodeId),
      adminSubtitleApi.getEvents(episodeId),
    ])
      .then(([analysisData, eventData]) => {
        if (!active) return;

        setAnalysis(analysisData);
        setEvents(eventData);
      })
      .catch((err) => {
        if (active) setError(err.message);
      });

    return () => {
      active = false;
    };
  }, [episodeId]);

  const selectedEpisode = drama?.episodes?.find(
    (episode) => episode.id === episodeId,
  );

  function selectEpisode(id) {
    if (busy) return;

    setEpisodeId(id);
    setAnalysis(null);
    setEvents([]);
    setFile(null);
    setError("");
    setMessage("");
  }

  async function perform(task, successMessage) {
    setBusy(true);
    setError("");
    setMessage("");

    try {
      const result = await task();
      setMessage(successMessage);
      return result;
    } catch (err) {
      setError(err.message);
      return null;
    } finally {
      setBusy(false);
    }
  }

  async function uploadSubtitle(event) {
    event.preventDefault();

    if (!file || !episodeId) {
      setError("SRT 파일을 선택해 주세요.");
      return;
    }

    const result = await perform(
      () => adminSubtitleApi.upload(episodeId, file),
      "자막 파일을 등록했습니다.",
    );

    if (result) {
      setAnalysis(result);
      setFile(null);
      event.currentTarget?.reset();
    }
  }

  async function requestAnalysis() {
    if (!episodeId) return;

    const result = await perform(
      () => adminSubtitleApi.requestAnalysis(episodeId),
      "분석 요청을 DB 대기열에 등록했습니다.",
    );

    if (result) setAnalysis(result);
  }

  async function addManualEvent(event) {
    event.preventDefault();

    if (!manualSummary.trim() || !episodeId) return;

    const result = await perform(
      () => adminSubtitleApi.createManualEvent(episodeId, manualSummary.trim()),
      "스포일러 사건을 등록했습니다.",
    );

    if (result) {
      setEvents((prev) => [result, ...prev]);
      setManualSummary("");
    }
  }

  async function reviewEvent(eventId, verified) {
    const result = await perform(
      () => adminSubtitleApi.reviewEvent(eventId, verified),
      verified ? "검수를 승인했습니다." : "승인을 취소했습니다.",
    );

    if (result) {
      setEvents((prev) =>
        prev.map((item) => (item.id === eventId ? result : item)),
      );
    }
  }

  function statusLabel() {
    if (!analysis?.subtitleFilename) return "자막 미등록";
    if (!analysis.requestedAt) return "분석 요청 전";

    const labels = {
      PENDING: "AI 분석 대기",
      PROCESSING: "AI 분석 진행 중",
      COMPLETED: "AI 분석 완료",
      FAILED: "AI 분석 실패",
      SKIPPED: "AI 분석 건너뜀",
    };

    return labels[analysis.analysisStatus] ?? analysis.analysisStatus;
  }

  if (!drama) {
    return (
      <main className="container main-content">
        {error || "작품 정보를 불러오는 중..."}
      </main>
    );
  }

  return (
    <main className="container main-content admin-page">
      <Link to="/admin/dramas" className="button button-outline">
        ← 작품 관리로
      </Link>

      <div className="section-top">
        <div>
          <span className="section-overline">SPOILOCK ADMIN</span>
          <h1>{drama.title} · 자막 및 AI 관리</h1>
          <p className="page-subtitle">총 {drama.episodes.length}화</p>
        </div>
      </div>

      <section className="admin-panel">
        <h2>회차 선택</h2>

        <div className="admin-episode-nav">
          {drama.episodes.map((episode) => (
            <button
              key={episode.id}
              type="button"
              className={
                episode.id === episodeId
                  ? "button button-primary"
                  : "button button-outline"
              }
              onClick={() => selectEpisode(episode.id)}
              disabled={busy}
            >
              {episode.episodeNumber}화
            </button>
          ))}
        </div>
      </section>

      {selectedEpisode && (
        <>
          <section className="admin-panel">
            <h2>
              {selectedEpisode.episodeNumber}화 · {selectedEpisode.title}
            </h2>

            <p className="page-subtitle">
              현재 상태: {analysis ? statusLabel() : "조회 중..."}
            </p>

            {analysis?.subtitleFilename && (
              <p>
                등록 자막: <strong>{analysis.subtitleFilename}</strong>
              </p>
            )}

            <form onSubmit={uploadSubtitle} className="admin-ai-form">
              <label htmlFor="subtitle-file">SRT 자막 파일</label>

              <input
                id="subtitle-file"
                type="file"
                accept=".srt"
                disabled={
                  busy || !analysis || Boolean(analysis.subtitleFilename)
                }
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              />

              <button
                type="submit"
                className="button button-outline"
                disabled={busy || !file || Boolean(analysis?.subtitleFilename)}
              >
                자막 업로드
              </button>
            </form>

            <button
              type="button"
              className="button button-primary"
              disabled={
                busy ||
                !analysis?.subtitleFilename ||
                (Boolean(analysis?.requestedAt) &&
                  analysis.analysisStatus !== "FAILED")
              }
              onClick={requestAnalysis}
            >
              AI 분석 요청
            </button>

            {analysis?.requestedAt && (
              <p className="page-subtitle">요청 시각: {analysis.requestedAt}</p>
            )}

            {analysis?.errorMessage && (
              <p className="form-error">{analysis.errorMessage}</p>
            )}

            <p className="page-subtitle">
              실제 AI 작업은 분석 워커 연동 후 실행됩니다.
            </p>
          </section>

          <section className="admin-panel">
            <h2>스포일러 이벤트 검수</h2>

            <form onSubmit={addManualEvent} className="admin-ai-form">
              <label htmlFor="event-summary">관리자 수동 사건 등록</label>

              <textarea
                id="event-summary"
                value={manualSummary}
                onChange={(e) => setManualSummary(e.target.value)}
                rows={3}
                maxLength={2000}
                placeholder="해당 회차에서 실제로 발생한 사건을 입력하세요."
                required
              />

              <button
                type="submit"
                className="button button-outline"
                disabled={busy}
              >
                사건 등록
              </button>
            </form>

            {events.length === 0 ? (
              <p className="page-subtitle">
                아직 등록된 스포일러 사건이 없습니다.
              </p>
            ) : (
              <div className="admin-event-list">
                {events.map((item) => (
                  <article className="admin-event-card" key={item.id}>
                    <div className="admin-heading">
                      <strong>사건 #{item.id}</strong>

                      <span className="admin-status">
                        {item.verified ? "검수 완료" : "검수 대기"}
                      </span>
                    </div>

                    <p>{item.eventSummary}</p>

                    <p className="page-subtitle">
                      출처: {item.sourceType}
                      {" · "}색인: {item.indexStatus}
                    </p>

                    <button
                      type="button"
                      className="button button-outline"
                      disabled={busy}
                      onClick={() => reviewEvent(item.id, !item.verified)}
                    >
                      {item.verified ? "승인 취소" : "검수 승인"}
                    </button>
                  </article>
                ))}
              </div>
            )}
          </section>
        </>
      )}

      {error && (
        <p role="alert" className="form-error">
          {error}
        </p>
      )}

      {message && (
        <p role="status" className="form-success">
          {message}
        </p>
      )}
    </main>
  );
}
