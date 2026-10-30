import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";

import { adminDramaApi } from "../api/adminDramaApi";
import { adminSubtitleApi } from "../api/adminSubtitleApi";
import AdminEpisodeVideoPanel from "../components/admin/AdminEpisodeVideoPanel";
import FileDropInput from "../components/common/FileDropInput";
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

// 사건 출처 (분석 원본)
const SOURCE_LABELS = {
  SUBTITLE: "자막",
  SUBTITLE_VIDEO: "자막 + 영상",
  VIDEO_STT: "영상 음성 인식",
  MANUAL: "수동 등록",
  LICENSED_SUBTITLE: "자막 (기존 데이터)",
};

// 사건 근거 종류
const EVIDENCE_LABELS = {
  DIALOGUE: "대사 근거",
  VISUAL: "화면 근거",
  BOTH: "대사 + 화면 근거",
};

// 검수 상태
const REVIEW_LABELS = {
  PENDING: "검수 대기",
  VERIFIED: "승인",
  REJECTED: "반려",
};

const REVIEW_STATUS_CLASS = {
  PENDING: "admin-subtitles__status--pending",
  VERIFIED: "admin-subtitles__status--success",
  REJECTED: "admin-subtitles__status--danger",
};

// 분석 원본 표시
const ANALYSIS_SOURCE_LABELS = {
  SUBTITLE: "자막",
  SUBTITLE_VIDEO: "자막 + 영상",
  VIDEO: "영상 음성 인식",
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

  // 분석 옵션
  const [videoRegistered, setVideoRegistered] = useState(false);
  const [withVideo, setWithVideo] = useState(true);
  const [replaceExisting, setReplaceExisting] = useState(false);

  // 사건별 최초 공개 회차 입력값 (승인 시 사용)
  const [revealDrafts, setRevealDrafts] = useState({});

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

  // 분석 작업이 대기·진행 중인지
  const analysisActive =
    Boolean(analysis?.requestedAt) &&
    (analysis?.analysisStatus === "PENDING" ||
      analysis?.analysisStatus === "PROCESSING");

  function getAnalysisLabel() {
    if (!analysis?.subtitleFilename && !analyzedByVideo) {
      return "자막 미등록";
    }

    if (!analysis.requestedAt) {
      return "분석 요청 전";
    }

    let label =
      ANALYSIS_LABELS[analysis.analysisStatus] ?? analysis.analysisStatus;

    if (analysis.analysisStatus === "PROCESSING" && analysis.progress != null) {
      label = `${label} ${analysis.progress}%`;
    }

    const source = ANALYSIS_SOURCE_LABELS[analysis.analysisSource];

    return source ? `${label} (${source})` : label;
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

    const useVideo = videoRegistered && withVideo;

    const result = await perform(
      () =>
        adminSubtitleApi.requestAnalysis(episodeId, {
          replace: replaceExisting,
          withVideo: useVideo,
        }),
      useVideo
        ? "자막 + 영상 분석을 시작했습니다. 영상 처리에 시간이 걸리니 새로고침으로 진행 상황을 확인해 주세요."
        : "자막 분석을 시작했습니다. 새로고침으로 진행 상황을 확인해 주세요.",
    );

    if (result) {
      setAnalysis(result);
      setReplaceExisting(false);
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
  // 승인 시 최초 공개 회차 기본값: 사건이 나온 회차
  function revealValue(item) {
    return (
      revealDrafts[item.id] ??
      item.firstRevealEpisode ??
      item.sourceEpisode ??
      1
    );
  }

  async function reviewEvent(item, status) {
    if (busy || loadingDetails) return;

    const eventId = item.id;
    const messages = {
      VERIFIED: `승인했습니다. ${revealValue(item)}화부터 시청한 사용자에게 공개됩니다.`,
      REJECTED: "반려했습니다. 이 사건은 스포일러 판정에 사용되지 않습니다.",
      PENDING: "검수 대기로 되돌렸습니다.",
    };

    const result = await perform(
      () =>
        adminSubtitleApi.reviewEvent(
          eventId,
          status,
          status === "VERIFIED" ? Number(revealValue(item)) : null,
        ),
      messages[status],
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

  // 분석 전 / 실패 / '기존 사건 교체'(재분석) 선택 시 요청 가능
  const canRequest =
    !busy &&
    !loadingDetails &&
    Boolean(analysis?.subtitleFilename) &&
    !analysisActive &&
    (!analysis?.requestedAt ||
      analysis.analysisStatus === "FAILED" ||
      replaceExisting);

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
            onVideoChange={setVideoRegistered}
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
                  <FileDropInput
                    file={file}
                    inputRef={fileInputRef}
                    accept=".srt"
                    extension=".srt"
                    maxBytes={MAX_SRT_SIZE}
                    description="SRT 자막 · 최대 2MB · 선택 후 자막 업로드를 눌러 주세요."
                    disabled={!canChooseFile}
                    onSelect={(selected) => {
                      clearMessages();
                      setFile(selected);
                    }}
                    onError={(text) => {
                      resetFileInput();
                      setMessage("");
                      setError(text);
                    }}
                  />
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
                    {videoRegistered && withVideo
                      ? "자막 + 영상 분석 요청"
                      : "AI 분석 요청"}
                  </button>
                </div>

                <div className="admin-subtitles__options">
                  {videoRegistered ? (
                    <label className="admin-subtitles__option">
                      <input
                        type="checkbox"
                        checked={withVideo}
                        disabled={busy}
                        onChange={(event) => setWithVideo(event.target.checked)}
                      />
                      영상 화면도 함께 분석 (위에 등록된 MP4 사용)
                    </label>
                  ) : (
                    <span className="admin-subtitles__option-note">
                      위에 MP4를 등록하면 영상 화면도 함께 분석할 수 있습니다.
                    </span>
                  )}

                  <label className="admin-subtitles__option">
                    <input
                      type="checkbox"
                      checked={replaceExisting}
                      disabled={busy}
                      onChange={(event) =>
                        setReplaceExisting(event.target.checked)
                      }
                    />
                    기존 사건 교체 (재분석 · 검수한 사건과 수동 사건은 유지)
                  </label>
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
                  분석은 AI 서버에서 진행되며, 끝나면 오른쪽에 사건 후보가
                  표시됩니다. 자막 + 영상 분석은 자막과 영상의 시간이 맞아야
                  합니다. 화면 근거가 있는 사건은 표시된 시각을 영상에서 직접
                  확인한 뒤 승인해 주세요.
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
                              (REVIEW_STATUS_CLASS[item.verificationStatus] ??
                                "admin-subtitles__status--pending")
                            }
                          >
                            {REVIEW_LABELS[item.verificationStatus] ??
                              "검수 대기"}
                            {item.verificationStatus === "VERIFIED" &&
                              item.firstRevealEpisode &&
                              ` · ${item.firstRevealEpisode}화부터`}
                          </span>
                        </div>

                        <p className="admin-subtitles__event-content">
                          {item.eventSummary}
                        </p>

                        <div className="admin-subtitles__event-meta">
                          <span>
                            출처:{" "}
                            {SOURCE_LABELS[item.sourceType] ?? item.sourceType}
                          </span>

                          {item.evidenceType && (
                            <span>
                              {EVIDENCE_LABELS[item.evidenceType] ??
                                item.evidenceType}
                            </span>
                          )}

                          <span>
                            색인:{" "}
                            {INDEX_LABELS[item.indexStatus] ?? item.indexStatus}
                          </span>
                        </div>

                        {item.visualEvidence?.length > 0 && (
                          <ul className="admin-subtitles__evidence">
                            {item.visualEvidence.map((v, index) => (
                              <li key={`v-${index}`}>
                                <strong>🎬 {v.timestamp}</strong>{" "}
                                {v.description}
                              </li>
                            ))}
                          </ul>
                        )}

                        {item.evidence?.length > 0 && (
                          <ul className="admin-subtitles__evidence">
                            {item.evidence.map((e, index) => (
                              <li key={`d-${index}`}>“{e.quote}”</li>
                            ))}
                          </ul>
                        )}

                        <div className="admin-subtitles__review">
                          {item.verificationStatus !== "VERIFIED" && (
                            <label className="admin-subtitles__reveal">
                              최초 공개
                              <input
                                type="number"
                                min={1}
                                max={item.sourceEpisode ?? undefined}
                                value={revealValue(item)}
                                disabled={busy || loadingDetails}
                                onChange={(event) =>
                                  setRevealDrafts((prev) => ({
                                    ...prev,
                                    [item.id]: event.target.value,
                                  }))
                                }
                              />
                              화
                            </label>
                          )}

                          {item.verificationStatus !== "VERIFIED" && (
                            <button
                              type="button"
                              className="admin-subtitles__button admin-subtitles__button--primary"
                              disabled={busy || loadingDetails}
                              onClick={() => reviewEvent(item, "VERIFIED")}
                            >
                              승인
                            </button>
                          )}

                          {item.verificationStatus !== "REJECTED" && (
                            <button
                              type="button"
                              className="admin-subtitles__button admin-subtitles__button--outline"
                              disabled={busy || loadingDetails}
                              onClick={() => reviewEvent(item, "REJECTED")}
                            >
                              반려
                            </button>
                          )}

                          {item.verificationStatus &&
                            item.verificationStatus !== "PENDING" && (
                              <button
                                type="button"
                                className="admin-subtitles__button admin-subtitles__button--outline"
                                disabled={busy || loadingDetails}
                                onClick={() => reviewEvent(item, "PENDING")}
                              >
                                대기로
                              </button>
                            )}
                        </div>
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
