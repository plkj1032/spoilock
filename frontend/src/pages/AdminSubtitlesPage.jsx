import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";

import { adminDramaApi } from "../api/adminDramaApi";
import { adminSubtitleApi } from "../api/adminSubtitleApi";
import AdminEpisodeVideoPanel from "../components/admin/AdminEpisodeVideoPanel";

import "../styles/AdminSubtitlesPage.css";

// SRT 최대 크기: 2MB
const MAX_SRT_SIZE = 2 * 1024 * 1024;

// AI 분석 상태
const ANALYSIS_LABELS = {
  PENDING: "분석 대기",
  PROCESSING: "분석 진행 중",
  COMPLETED: "분석 완료",
  FAILED: "분석 실패",
  SKIPPED: "분석 건너뜀",
};

// ChromaDB 색인 상태
const INDEX_LABELS = {
  PENDING: "색인 대기",
  INDEXED: "색인 완료",
  FAILED: "색인 실패",
};

// 날짜 출력
function formatDateTime(value) {
  if (!value) return "-";

  return String(value).slice(0, 19).replace("T", " ");
}

// 작품 ID가 변경되면 페이지 상태를 새로 시작
export default function AdminSubtitlesPage() {
  const { dramaId } = useParams();

  return <AdminSubtitlesContent key={dramaId} dramaId={dramaId} />;
}

function AdminSubtitlesContent({ dramaId }) {
  const fileInputRef = useRef(null);

  // ==========================================
  // 1. 드라마 및 선택 회차
  // ==========================================
  const [drama, setDrama] = useState(null);
  const [episodeId, setEpisodeId] = useState(null);

  // ==========================================
  // 2. AI 분석 및 스포일러 사건
  // ==========================================
  const [analysis, setAnalysis] = useState(null);
  const [events, setEvents] = useState([]);

  // ==========================================
  // 3. 입력값
  // ==========================================
  const [file, setFile] = useState(null);
  const [manualSummary, setManualSummary] = useState("");

  // ==========================================
  // 4. 로딩 상태
  // 초기 로딩은 useState에서 설정
  // ==========================================
  const [loadingDrama, setLoadingDrama] = useState(true);
  const [loadingDetails, setLoadingDetails] = useState(true);
  const [busy, setBusy] = useState(false);

  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  // 새로고침 시 재조회
  const [reloadKey, setReloadKey] = useState(0);

  // ==========================================
  // 5. 드라마 및 회차 목록 조회
  // ==========================================
  useEffect(() => {
    let active = true;

    adminDramaApi
      .getDetail(dramaId)
      .then((data) => {
        if (!active) return;

        const episodes = [...(data.episodes ?? [])].sort(
          (a, b) => a.episodeNumber - b.episodeNumber,
        );

        setDrama({
          ...data,
          episodes,
        });

        // 첫 번째 회차 선택
        setEpisodeId(episodes[0]?.id ?? null);
      })
      .catch((err) => {
        if (active) {
          setError(err.message);
        }
      })
      .finally(() => {
        if (active) {
          setLoadingDrama(false);
        }
      });

    return () => {
      active = false;
    };
  }, [dramaId]);

  // ==========================================
  // 6. 선택 회차 AI 정보 및 사건 조회
  // ==========================================
  useEffect(() => {
    if (episodeId == null) return;

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
        if (active) {
          setError(err.message);
        }
      })
      .finally(() => {
        if (active) {
          setLoadingDetails(false);
        }
      });

    return () => {
      active = false;
    };
  }, [episodeId, reloadKey]);

  // ==========================================
  // 7. 선택된 회차 찾기
  // ==========================================
  const selectedEpisode = drama?.episodes?.find(
    (episode) => episode.id === episodeId,
  );

  // ==========================================
  // 8. 분석 상태 한글 표시
  // ==========================================
  // 영상으로 분석한 회차는 자막이 없어도 분석 상태를 표시
  const analyzedByVideo = analysis?.analysisSource === "VIDEO";

  function getAnalysisLabel() {
    if (!analysis?.subtitleFilename && !analyzedByVideo) {
      return "자막 미등록";
    }

    if (!analysis.requestedAt) {
      return "분석 요청 전";
    }

    const label =
      ANALYSIS_LABELS[analysis.analysisStatus] ?? analysis.analysisStatus;

    return analyzedByVideo ? `${label} (영상)` : label;
  }

  function getStatusType() {
    if (!analysis?.subtitleFilename && !analyzedByVideo) {
      return "idle";
    }

    if (!analysis.requestedAt) {
      return "idle";
    }

    if (analysis.analysisStatus === "COMPLETED") {
      return "success";
    }

    if (analysis.analysisStatus === "FAILED") {
      return "danger";
    }

    return "pending";
  }

  // ==========================================
  // 9. 알림 초기화
  // ==========================================
  function clearMessages() {
    setError("");
    setMessage("");
  }

  // ==========================================
  // 10. 파일 선택 초기화
  // ==========================================
  function resetFileInput() {
    setFile(null);

    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  }

  // ==========================================
  // 11. 회차 변경
  // useEffect 밖에서 로딩 상태 변경
  // ==========================================
  function selectEpisode(id) {
    if (busy || loadingDetails || id === episodeId) {
      return;
    }

    clearMessages();

    // 회차 선택 이벤트에서 로딩 시작
    setLoadingDetails(true);

    setEpisodeId(id);
    setAnalysis(null);
    setEvents([]);
    setManualSummary("");

    resetFileInput();
  }

  // ==========================================
  // 12. SRT 파일 선택
  // ==========================================
  function handleFileChange(event) {
    clearMessages();

    const selected = event.target.files?.[0] ?? null;

    if (!selected) {
      setFile(null);
      return;
    }

    if (!selected.name.toLowerCase().endsWith(".srt")) {
      setError("SRT 확장자의 자막 파일만 선택할 수 있습니다.");

      resetFileInput();
      return;
    }

    if (selected.size === 0 || selected.size > MAX_SRT_SIZE) {
      setError("2MB 이하의 비어 있지 않은 SRT 파일을 선택해 주세요.");

      resetFileInput();
      return;
    }

    setFile(selected);
  }

  // ==========================================
  // 13. 공통 API 작업 처리
  // ==========================================
  async function perform(task, successMessage) {
    if (busy) return null;

    setBusy(true);
    clearMessages();

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

  // ==========================================
  // 14. SRT 업로드
  // ==========================================
  async function uploadSubtitle(event) {
    event.preventDefault();

    if (!file || !episodeId || busy || loadingDetails) {
      return;
    }

    const result = await perform(
      () => adminSubtitleApi.upload(episodeId, file),
      "자막 파일이 정상적으로 등록되었습니다.",
    );

    if (result) {
      setAnalysis(result);
      resetFileInput();
    }
  }

  // ==========================================
  // 15. AI 분석 요청
  // ==========================================
  async function requestAnalysis() {
    if (!episodeId || busy || loadingDetails) {
      return;
    }

    const result = await perform(
      () => adminSubtitleApi.requestAnalysis(episodeId),
      "AI 분석 요청이 DB에 등록되었습니다.",
    );

    if (result) {
      setAnalysis(result);
    }
  }

  // ==========================================
  // 16. 스포일러 사건 수동 등록
  // ==========================================
  async function addManualEvent(event) {
    event.preventDefault();

    const summary = manualSummary.trim();

    if (!summary || !episodeId || busy || loadingDetails) {
      return;
    }

    const result = await perform(
      () => adminSubtitleApi.createManualEvent(episodeId, summary),
      "스포일러 사건이 등록되었습니다.",
    );

    if (result) {
      setEvents((prev) => [result, ...prev]);
      setManualSummary("");
    }
  }

  // ==========================================
  // 17. 사건 승인 / 승인 취소
  // ==========================================
  async function reviewEvent(eventId, verified) {
    if (busy || loadingDetails) return;

    const result = await perform(
      () => adminSubtitleApi.reviewEvent(eventId, verified),
      verified
        ? "스포일러 사건 검수를 승인했습니다."
        : "스포일러 사건 승인을 취소했습니다.",
    );

    if (result) {
      setEvents((prev) =>
        prev.map((item) => (item.id === eventId ? result : item)),
      );
    }
  }

  // ==========================================
  // 18. AI 상태 및 사건 새로고침
  // useEffect 밖에서 로딩 시작
  // ==========================================
  function refreshDetails() {
    if (busy || loadingDetails || !episodeId) return;

    clearMessages();

    setLoadingDetails(true);
    setReloadKey((prev) => prev + 1);
  }

  // ==========================================
  // 19. 버튼 활성화 조건
  // ==========================================
  const canChooseFile =
    !busy &&
    !loadingDetails &&
    Boolean(analysis) &&
    !analysis?.subtitleFilename;

  const canUpload = canChooseFile && Boolean(file);

  const canRequest =
    !busy &&
    !loadingDetails &&
    Boolean(analysis?.subtitleFilename) &&
    (!analysis?.requestedAt || analysis.analysisStatus === "FAILED");

  // ==========================================
  // 20. 드라마 로딩
  // ==========================================
  if (loadingDrama) {
    return (
      <main className="container main-content admin-subtitles-page">
        <section className="admin-subtitles__card">
          <p className="admin-subtitles__muted">
            작품 정보를 불러오는 중입니다...
          </p>
        </section>
      </main>
    );
  }

  // ==========================================
  // 21. 드라마 조회 실패
  // ==========================================
  if (!drama) {
    return (
      <main className="container main-content admin-subtitles-page">
        <section className="admin-subtitles__card">
          <h1>작품 정보를 불러오지 못했습니다.</h1>

          <p
            className="admin-subtitles__alert admin-subtitles__alert--error"
            role="alert"
          >
            {error || "존재하지 않는 작품입니다."}
          </p>

          <Link
            to="/admin/dramas"
            className="admin-subtitles__button admin-subtitles__button--outline"
          >
            작품 목록으로
          </Link>
        </section>
      </main>
    );
  }

  // ==========================================
  // 22. 실제 페이지
  // ==========================================
  return (
    <main className="container main-content admin-subtitles-page">
      {/* ==================================
          상단 헤더
      =================================== */}
      <header className="admin-subtitles__header">
        <Link to="/admin/dramas" className="admin-subtitles__back">
          ← 작품 관리로 돌아가기
        </Link>

        <div className="admin-subtitles__header-row">
          <div className="admin-subtitles__header-text">
            <span className="admin-subtitles__eyebrow">SPOILOCK ADMIN</span>

            <h1>콘텐츠 · AI 관리</h1>

            <p className="admin-subtitles__muted">
              {drama.title}의 영상, 자막과 AI 분석 결과를 관리합니다.
            </p>
          </div>

          <div className="admin-subtitles__header-meta">
            <span className="admin-subtitles__info-pill">
              총 {drama.episodes.length}화
            </span>

            <span className="admin-subtitles__info-pill">작품 #{drama.id}</span>
          </div>
        </div>
      </header>

      {/* ==================================
          오류 / 성공 알림
      =================================== */}
      {error && (
        <div
          className="admin-subtitles__alert admin-subtitles__alert--error"
          role="alert"
        >
          {error}
        </div>
      )}

      {message && (
        <div
          className="admin-subtitles__alert admin-subtitles__alert--success"
          role="status"
        >
          {message}
        </div>
      )}

      {/* ==================================
          회차 선택
      =================================== */}
      <section className="admin-subtitles__card admin-subtitles__episodes-card">
        <div className="admin-subtitles__section-heading">
          <div>
            <h2>회차 선택</h2>

            <p className="admin-subtitles__muted">
              콘텐츠를 관리할 회차를 선택하세요.
            </p>
          </div>
        </div>

        <div className="admin-subtitles__episode-list">
          {drama.episodes.map((episode) => (
            <button
              key={episode.id}
              type="button"
              className={
                "admin-subtitles__episode-button" +
                (episode.id === episodeId
                  ? " admin-subtitles__episode-button--active"
                  : "")
              }
              onClick={() => selectEpisode(episode.id)}
              disabled={busy || loadingDetails}
              aria-pressed={episode.id === episodeId}
            >
              {episode.episodeNumber}화
            </button>
          ))}
        </div>
      </section>

      {/* ==================================
          선택된 회차의 관리 기능
      =================================== */}
      {selectedEpisode ? (
        <>
          {/* ==================================
              MP4 영상 관리
          =================================== */}
          <AdminEpisodeVideoPanel
            key={selectedEpisode.id}
            episodeId={selectedEpisode.id}
            onAnalysisRequested={() => setReloadKey((prev) => prev + 1)}
          />

          {/* ==================================
              SRT · AI · 스포일러 검수
          =================================== */}
          <div className="admin-subtitles__grid">
            {/* ==============================
                왼쪽: 자막 및 AI
            ============================== */}
            <section className="admin-subtitles__card">
              <div className="admin-subtitles__section-heading">
                <div>
                  <span className="admin-subtitles__eyebrow">
                    SUBTITLE & AI
                  </span>

                  <h2>
                    {selectedEpisode.episodeNumber}화 · {selectedEpisode.title}
                  </h2>
                </div>

                <span
                  className={
                    "admin-subtitles__status " +
                    `admin-subtitles__status--${getStatusType()}`
                  }
                >
                  {loadingDetails ? "조회 중" : getAnalysisLabel()}
                </span>
              </div>

              {/* 등록 자막 정보 */}
              <div className="admin-subtitles__info-box">
                <span className="admin-subtitles__field-label">
                  등록된 자막
                </span>

                <strong className="admin-subtitles__filename">
                  {loadingDetails
                    ? "조회 중..."
                    : (analysis?.subtitleFilename ??
                      "아직 등록된 자막이 없습니다.")}
                </strong>

                <span className="admin-subtitles__muted">
                  SRT 형식 · UTF-8 · 최대 2MB
                </span>
              </div>

              {/* 자막 업로드 */}
              <form
                className="admin-subtitles__upload-form"
                onSubmit={uploadSubtitle}
              >
                <h3>자막 파일 등록</h3>

                <div className="admin-subtitles__file-picker">
                  <input
                    ref={fileInputRef}
                    id="admin-subtitles-file"
                    className="admin-subtitles__file-input"
                    type="file"
                    accept=".srt"
                    disabled={!canChooseFile}
                    onChange={handleFileChange}
                  />

                  <label
                    htmlFor="admin-subtitles-file"
                    className={
                      "admin-subtitles__file-label" +
                      (!canChooseFile
                        ? " admin-subtitles__file-label--disabled"
                        : "")
                    }
                  >
                    파일 선택
                  </label>

                  <span className="admin-subtitles__selected-file">
                    {file?.name ?? "선택된 파일 없음"}
                  </span>
                </div>

                <div className="admin-subtitles__actions">
                  <button
                    type="submit"
                    className="admin-subtitles__button admin-subtitles__button--outline"
                    disabled={!canUpload}
                  >
                    {busy ? "처리 중..." : "자막 업로드"}
                  </button>

                  <button
                    type="button"
                    className="admin-subtitles__button admin-subtitles__button--primary"
                    disabled={!canRequest}
                    onClick={requestAnalysis}
                  >
                    AI 분석 요청
                  </button>
                </div>
              </form>

              {/* AI 분석 상태 */}
              <div className="admin-subtitles__analysis-box">
                <div className="admin-subtitles__analysis-title">
                  <h3>AI 분석 상태</h3>

                  <button
                    type="button"
                    className="admin-subtitles__refresh"
                    onClick={refreshDetails}
                    disabled={busy || loadingDetails}
                  >
                    {loadingDetails ? "조회 중..." : "새로고침"}
                  </button>
                </div>

                <div className="admin-subtitles__detail-row">
                  <span>상태</span>
                  <strong>
                    {loadingDetails ? "조회 중..." : getAnalysisLabel()}
                  </strong>
                </div>

                <div className="admin-subtitles__detail-row">
                  <span>요청 시각</span>
                  <strong>{formatDateTime(analysis?.requestedAt)}</strong>
                </div>

                <div className="admin-subtitles__detail-row">
                  <span>분석 완료 시각</span>
                  <strong>{formatDateTime(analysis?.analyzedAt)}</strong>
                </div>

                {analysis?.errorMessage && (
                  <div
                    className="admin-subtitles__alert admin-subtitles__alert--error"
                    role="alert"
                  >
                    {analysis.errorMessage}
                  </div>
                )}

                <p className="admin-subtitles__help">
                  현재 AI 분석 요청은 DB에 등록됩니다. 실제 분석은 FastAPI 워커
                  연동 후 실행됩니다.
                </p>
              </div>
            </section>

            {/* ==============================
                오른쪽: 스포일러 이벤트 검수
            ============================== */}
            <section className="admin-subtitles__card">
              <div className="admin-subtitles__section-heading">
                <div>
                  <span className="admin-subtitles__eyebrow">
                    SPOILER REVIEW
                  </span>

                  <h2>스포일러 이벤트 검수</h2>

                  <p className="admin-subtitles__muted">
                    회차별 주요 사건을 등록하고 검수합니다.
                  </p>
                </div>
              </div>

              {/* 수동 사건 등록 */}
              <form
                className="admin-subtitles__event-form"
                onSubmit={addManualEvent}
              >
                <label htmlFor="admin-event-summary">
                  관리자 수동 사건 등록
                </label>

                <textarea
                  id="admin-event-summary"
                  rows={5}
                  maxLength={2000}
                  value={manualSummary}
                  onChange={(e) => setManualSummary(e.target.value)}
                  placeholder="예: 2화에서 주인공이 사건의 진범을 알아낸다."
                  disabled={busy || loadingDetails}
                  required
                />

                <div className="admin-subtitles__form-footer">
                  <span className="admin-subtitles__muted">
                    {manualSummary.length} / 2000자
                  </span>

                  <button
                    type="submit"
                    className="admin-subtitles__button admin-subtitles__button--primary"
                    disabled={busy || loadingDetails || !manualSummary.trim()}
                  >
                    사건 등록
                  </button>
                </div>
              </form>

              {/* 등록 사건 목록 */}
              <div className="admin-subtitles__events-section">
                <div className="admin-subtitles__events-heading">
                  <h3>등록된 사건</h3>

                  <span className="admin-subtitles__count">
                    {loadingDetails ? "-" : events.length}건
                  </span>
                </div>

                {loadingDetails && (
                  <p className="admin-subtitles__muted">
                    사건 목록을 불러오는 중입니다...
                  </p>
                )}

                {!loadingDetails && events.length === 0 && (
                  <div className="admin-subtitles__empty">
                    <strong>등록된 사건이 없습니다.</strong>

                    <p>
                      AI 분석 결과가 등록되거나 관리자가 직접 사건을 추가하면
                      이곳에 표시됩니다.
                    </p>
                  </div>
                )}

                {!loadingDetails && events.length > 0 && (
                  <div className="admin-subtitles__event-list">
                    {events.map((item) => (
                      <article className="admin-subtitles__event" key={item.id}>
                        <div className="admin-subtitles__event-top">
                          <strong>사건 #{item.id}</strong>

                          <span
                            className={
                              "admin-subtitles__status " +
                              (item.verified
                                ? "admin-subtitles__status--success"
                                : "admin-subtitles__status--pending")
                            }
                          >
                            {item.verified ? "검수 완료" : "검수 대기"}
                          </span>
                        </div>

                        <p className="admin-subtitles__event-content">
                          {item.eventSummary}
                        </p>

                        <div className="admin-subtitles__event-meta">
                          <span>출처: {item.sourceType}</span>

                          <span>
                            색인:{" "}
                            {INDEX_LABELS[item.indexStatus] ?? item.indexStatus}
                          </span>
                        </div>

                        <button
                          type="button"
                          className={
                            "admin-subtitles__button " +
                            (item.verified
                              ? "admin-subtitles__button--outline"
                              : "admin-subtitles__button--primary")
                          }
                          disabled={busy || loadingDetails}
                          onClick={() => reviewEvent(item.id, !item.verified)}
                        >
                          {item.verified ? "승인 취소" : "검수 승인"}
                        </button>
                      </article>
                    ))}
                  </div>
                )}
              </div>
            </section>
          </div>
        </>
      ) : (
        <section className="admin-subtitles__card">
          <p className="admin-subtitles__muted">등록된 회차가 없습니다.</p>
        </section>
      )}
    </main>
  );
}
