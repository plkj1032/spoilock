import { useCallback, useEffect, useRef, useState } from "react";
import { dramaWatchApi } from "../../api/dramaWatchApi";

export default function EpisodePlayer({ dramaId, episode }) {
  const videoRef = useRef(null);
  const activeRef = useRef(false);
  const readyRef = useRef(false);
  const playedRef = useRef(false);

  const snapshotRef = useRef(null);
  const lastQueuedRef = useRef("");

  const [source, setSource] = useState(null);
  const [error, setError] = useState("");
  const [saveError, setSaveError] = useState("");
  const [resumeMessage, setResumeMessage] = useState("");
  const [revision, setRevision] = useState(0);

  // ==========================================
  // 1. 동영상 기본 메뉴 차단
  // ==========================================
  function preventVideoContextMenu(event) {
    event.preventDefault();
  }

  function preventVideoDrag(event) {
    event.preventDefault();
  }

  // ==========================================
  // 2. 현재 재생 위치 수집
  // ==========================================
  function capturePosition(video = videoRef.current) {
    if (!video || !readyRef.current || !playedRef.current) return;
    if (video.seeking) return;

    const duration = video.duration;
    const position = video.currentTime;

    if (
      !Number.isFinite(duration) ||
      !Number.isFinite(position) ||
      duration <= 0
    ) {
      return;
    }

    snapshotRef.current = {
      positionSeconds: Math.min(duration, Math.max(0, position)),
      durationSeconds: duration,
    };
  }

  // ==========================================
  // 3. 재생 위치 저장
  // ==========================================

  const saveLatestPosition = useCallback(() => {
    const snapshot = snapshotRef.current;
    if (!snapshot) return;

    const signature = JSON.stringify(snapshot);

    if (signature === lastQueuedRef.current) return;

    lastQueuedRef.current = signature;

    void dramaWatchApi
      .savePosition(dramaId, episode.episodeId, snapshot)
      .then(() => {
        if (activeRef.current) {
          setSaveError("");
        }
      })
      .catch(() => {
        if (lastQueuedRef.current === signature) {
          lastQueuedRef.current = "";
        }

        if (activeRef.current) {
          setSaveError(
            "이어보기 위치를 저장하지 못했어요. 로그인과 연결 상태를 확인해 주세요.",
          );
        }
      });
  }, [dramaId, episode.episodeId]);

  // ==========================================
  // 4. 재생 중 10초마다 위치 저장
  // ==========================================

  useEffect(() => {
    activeRef.current = true;

    function saveBeforeBackground() {
      if (document.visibilityState === "hidden") {
        capturePosition();
        saveLatestPosition();
      }
    }

    const timer = window.setInterval(() => {
      const video = videoRef.current;

      if (video && !video.paused && !video.ended) {
        capturePosition(video);
        saveLatestPosition();
      }
    }, 10000);

    document.addEventListener("visibilitychange", saveBeforeBackground);

    return () => {
      activeRef.current = false;

      window.clearInterval(timer);

      document.removeEventListener("visibilitychange", saveBeforeBackground);

      saveLatestPosition();
    };
  }, [dramaId, episode.episodeId, saveLatestPosition]);

  // ==========================================
  // 5. 영상 정보 및 이어보기 조회
  // ==========================================
  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    readyRef.current = false;
    playedRef.current = false;

    async function load() {
      try {
        const [playback, position] = await Promise.all([
          dramaWatchApi.getPlayback(
            dramaId,
            episode.episodeId,
            controller.signal,
          ),
          dramaWatchApi.getPosition(
            dramaId,
            episode.episodeId,
            controller.signal,
          ),
        ]);

        if (!active) return;

        setSource({ playback, position });
      } catch (err) {
        if (active && !controller.signal.aborted) {
          setError(err.message || "영상과 이어보기 정보를 불러오지 못했어요.");
        }
      }
    }

    void load();

    return () => {
      active = false;
      controller.abort();
    };
  }, [dramaId, episode.episodeId, revision]);

  // ==========================================
  // 6. 재생 다시 시도
  // ==========================================
  function retryPlayback() {
    saveLatestPosition();

    readyRef.current = false;
    playedRef.current = false;

    setSource(null);
    setError("");
    setResumeMessage("");

    setRevision((value) => value + 1);
  }

  // ==========================================
  // 7. 이전 시청 위치 복원
  // ==========================================
  function restorePosition(video) {
    const duration = video.duration;

    if (!Number.isFinite(duration) || duration <= 0) return;

    const savedPosition = source?.position;

    const seconds = savedPosition?.saved ? savedPosition.positionSeconds : 0;

    const target =
      Number.isFinite(seconds) && seconds > 0 && seconds < duration - 1
        ? seconds
        : 0;

    if (target > 0) {
      video.currentTime = target;

      const minutes = Math.floor(target / 60);
      const remainingSeconds = Math.floor(target % 60);

      setResumeMessage(
        `${minutes}분 ${remainingSeconds}초부터 이어볼 수 있어요.`,
      );
    } else {
      setResumeMessage("");
    }

    readyRef.current = true;
  }

  // ==========================================
  // 8. 즉시 재생 위치 저장
  // ==========================================
  function saveCurrentPosition(video) {
    capturePosition(video);
    saveLatestPosition();
  }

  // ==========================================
  // 9. 화면
  // ==========================================
  return (
    <section className="watch-player-panel" aria-label="영상 플레이어">
      {error ? (
        <div className="watch-player-placeholder">
          <p role="alert">{error}</p>

          <button type="button" onClick={retryPlayback}>
            재생 다시 시도
          </button>
        </div>
      ) : !source ? (
        <div className="watch-player-placeholder" role="status">
          영상과 이어보기 정보를 불러오고 있어요…
        </div>
      ) : (
        <video
          ref={videoRef}
          key={source.playback.playbackUrl}
          className="watch-video"
          // 기본 재생 컨트롤 유지
          controls
          playsInline
          preload="metadata"
          // 다운로드 및 원격 재생 버튼 제한
          controlsList="nodownload noremoteplayback"
          disableRemotePlayback
          // PIP 모드 제한
          disablePictureInPicture
          // 우클릭 메뉴 차단
          onContextMenu={preventVideoContextMenu}
          // 드래그 차단
          onDragStart={preventVideoDrag}
          // 영상 재생 주소
          src={source.playback.playbackUrl}
          aria-label={`${episode.episodeNumber}화 ${episode.title}`}
          // 메타데이터 로드 후 이어보기
          onLoadedMetadata={(event) => {
            restorePosition(event.currentTarget);
          }}
          // 재생 시작
          onPlay={(event) => {
            playedRef.current = true;
            capturePosition(event.currentTarget);
          }}
          // 재생 위치 변경
          onTimeUpdate={(event) => {
            capturePosition(event.currentTarget);
          }}
          // 일시정지 시 저장
          onPause={(event) => {
            saveCurrentPosition(event.currentTarget);
          }}
          // 탐색 후 저장
          onSeeked={(event) => {
            saveCurrentPosition(event.currentTarget);
          }}
          // 영상 종료 시 저장
          onEnded={(event) => {
            saveCurrentPosition(event.currentTarget);
          }}
          // 재생 오류
          onError={() => {
            saveLatestPosition();
            readyRef.current = false;

            setError(
              "영상을 재생하지 못했습니다. 재생 주소 만료 또는 영상 형식 문제일 수 있습니다.",
            );
          }}
        >
          브라우저가 영상 재생을 지원하지 않습니다.
        </video>
      )}

      {/* 현재 재생 회차 */}
      <div className="watch-current-episode">
        <span>현재 선택한 회차</span>

        <h2>
          {episode.episodeNumber}화 · {episode.title}
        </h2>

        {resumeMessage && (
          <p className="watch-success" role="status">
            {resumeMessage}
          </p>
        )}

        {saveError && (
          <div className="watch-inline-error" role="status">
            <span>{saveError}</span>

            <button
              type="button"
              onClick={() => {
                capturePosition();
                saveLatestPosition();
              }}
            >
              저장 다시 시도
            </button>
          </div>
        )}
      </div>
    </section>
  );
}
