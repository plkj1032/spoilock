import { useEffect, useRef, useState } from "react";

import { adminVideoApi } from "../../api/adminVideoApi";

import "../../styles/AdminEpisodeVideoPanel.css";

const MAX_VIDEO_SIZE = 100 * 1024 * 1024;

function formatSize(bytes) {
  if (bytes == null) return "-";

  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(value) {
  if (!value) return "-";

  return String(value).slice(0, 19).replace("T", " ");
}

export default function AdminEpisodeVideoPanel({ episodeId }) {
  const fileInputRef = useRef(null);

  const [video, setVideo] = useState(null);
  const [file, setFile] = useState(null);

  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  // =========================================
  // 등록 영상 정보 조회
  // =========================================

  useEffect(() => {
    let active = true;

    // 회차별 등록 영상 조회
    adminVideoApi
      .getVideo(episodeId)
      .then((data) => {
        if (!active) return;

        setVideo(data);
      })
      .catch((err) => {
        if (!active) return;

        setError(err.message);
      })
      .finally(() => {
        if (!active) return;

        setLoading(false);
      });

    // 컴포넌트 언마운트 시 이전 API 응답 무시
    return () => {
      active = false;
    };
  }, [episodeId]);

  // =========================================
  // 파일 입력 초기화
  // =========================================
  function resetFile() {
    setFile(null);

    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  }

  // =========================================
  // 파일 선택
  // =========================================
  function handleFileChange(event) {
    setError("");
    setMessage("");

    const selected = event.target.files?.[0];

    if (!selected) {
      setFile(null);
      return;
    }

    if (!selected.name.toLowerCase().endsWith(".mp4")) {
      setError("MP4 영상 파일을 선택해 주세요.");
      resetFile();
      return;
    }

    if (selected.size === 0 || selected.size > MAX_VIDEO_SIZE) {
      setError("100MB 이하의 MP4 파일을 선택해 주세요.");
      resetFile();
      return;
    }

    setFile(selected);
  }

  // =========================================
  // MP4 업로드
  // =========================================
  async function uploadVideo(event) {
    event.preventDefault();

    if (!file || !episodeId || busy) return;

    setBusy(true);
    setError("");
    setMessage("");

    try {
      const result = await adminVideoApi.upload(episodeId, file);

      setVideo(result);
      resetFile();

      setMessage("MP4 영상이 등록되었습니다.");
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  // =========================================
  // MP4 삭제
  // =========================================
  async function deleteVideo() {
    if (busy || !video?.registered) return;

    const confirmed = window.confirm(
      "등록된 영상을 삭제하시겠습니까?\n" + "이 작업은 되돌릴 수 없습니다.",
    );

    if (!confirmed) return;

    setBusy(true);
    setError("");
    setMessage("");

    try {
      await adminVideoApi.deleteVideo(episodeId);

      setVideo({
        episodeId,
        registered: false,
        filename: null,
        sizeBytes: null,
        uploadedAt: null,
      });

      resetFile();
      setMessage("등록된 영상을 삭제했습니다.");
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const hasVideo = Boolean(video?.registered);

  return (
    <section className="admin-subtitles__card admin-video">
      <div className="admin-video__heading">
        <div>
          <span className="admin-video__eyebrow">VIDEO MANAGEMENT</span>

          <h2>MP4 영상 관리</h2>

          <p className="admin-video__description">
            회차별 영상을 등록하고 저장 상태를 관리합니다.
          </p>
        </div>

        <span
          className={
            "admin-video__status " +
            (hasVideo ? "admin-video__status--ready" : "")
          }
        >
          {loading ? "조회 중" : hasVideo ? "등록 완료" : "미등록"}
        </span>
      </div>

      {error && (
        <p
          className="admin-video__alert admin-video__alert--error"
          role="alert"
        >
          {error}
        </p>
      )}

      {message && (
        <p
          className="admin-video__alert admin-video__alert--success"
          role="status"
        >
          {message}
        </p>
      )}

      {loading ? (
        <p className="admin-video__description">
          영상 정보를 불러오는 중입니다...
        </p>
      ) : hasVideo ? (
        <div className="admin-video__registered">
          <div className="admin-video__file-icon">
            <span aria-hidden="true">▶</span>
          </div>

          <div className="admin-video__file-info">
            <strong>{video.filename}</strong>

            <p>
              {formatSize(video.sizeBytes)}
              {" · "}
              {formatDate(video.uploadedAt)}
            </p>

            <span className="admin-video__file-note">
              서버에 저장된 MP4 파일입니다.
            </span>
          </div>

          <button
            type="button"
            className="admin-video__delete"
            disabled={busy}
            onClick={deleteVideo}
          >
            {busy ? "처리 중..." : "영상 삭제"}
          </button>
        </div>
      ) : (
        <form className="admin-video__upload-form" onSubmit={uploadVideo}>
          <div className="admin-video__upload-area">
            <div className="admin-video__upload-icon" aria-hidden="true">
              ↑
            </div>

            <strong>회차 영상 등록</strong>

            <p>
              MP4 형식의 영상 파일을 선택해 주세요.
              <span> 최대 100MB</span>
            </p>

            <input
              ref={fileInputRef}
              id={`episode-video-${episodeId}`}
              type="file"
              accept=".mp4,video/mp4"
              className="admin-video__file-input"
              onChange={handleFileChange}
              disabled={busy}
            />

            <label
              className="admin-video__file-button"
              htmlFor={`episode-video-${episodeId}`}
            >
              MP4 파일 선택
            </label>

            <span className="admin-video__selected-file">
              {file
                ? `${file.name} (${formatSize(file.size)})`
                : "선택된 파일이 없습니다."}
            </span>
          </div>

          <button
            type="submit"
            className="admin-video__submit"
            disabled={!file || busy}
          >
            {busy ? "영상 업로드 중..." : "영상 업로드"}
          </button>
        </form>
      )}

      <p className="admin-video__help">
        영상 등록과 AI 분석은 별개의 단계입니다. FastAPI 연동 전까지는 영상 저장
        상태만 관리합니다.
      </p>
    </section>
  );
}
