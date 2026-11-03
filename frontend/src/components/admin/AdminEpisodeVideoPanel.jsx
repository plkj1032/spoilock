import { useEffect, useRef, useState } from "react";

import { adminVideoApi } from "../../api/adminVideoApi";
import FileDropInput from "../common/FileDropInput";
import FileUploadStatus from "../common/FileUploadStatus";
import OperationLoading from "../common/OperationLoading";
import "../../styles/AdminEpisodeVideoPanel.css";

const MAX_VIDEO_SIZE = 400 * 1024 * 1024;

export default function AdminEpisodeVideoPanel({
  episodeId,
  onAnalysisRequested,
  onVideoChange,
  uploadState,
  onUpload,
}) {
  const fileInputRef = useRef(null);

  const [video, setVideo] = useState(null);
  const [file, setFile] = useState(null);

  const [loading, setLoading] = useState(true);
  const [operationBusy, setBusy] = useState(false);

  const uploading = uploadState?.status === "uploading";
  const busy = operationBusy || uploading;
  const [operation, setOperation] = useState("");

  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  // 이미 사건이 있는 회차를 다시 분석할지
  const [replace, setReplace] = useState(false);

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
  // 영상 등록 여부를 상위(자막·분석 화면)에 알림
  // → '영상 화면도 함께 분석' 옵션 표시
  // =========================================
  useEffect(() => {
    onVideoChange?.(Boolean(video?.registered));
  }, [video, onVideoChange]);

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
  // MP4 업로드
  // =========================================
  function uploadVideo(event) {
    event.preventDefault();

    if (!file || !episodeId || busy) return;

    setError("");
    setMessage("");

    // 실제 요청과 결과는 부모가 관리하므로 회차를 바꿔도 유지됨
    onUpload(episodeId, file);
  }

  // =========================================
  // 등록된 영상으로 사건 분석 요청
  // =========================================
  async function requestAnalysis() {
    if (busy || !video?.registered) return;

    setBusy(true);
    setOperation("analysis");
    setError("");
    setMessage("");

    try {
      const result = await adminVideoApi.requestAnalysis(episodeId, replace);

      setMessage("영상 분석 요청을 접수했습니다.");
      onAnalysisRequested?.(result);
    } catch (err) {
      setError(
        err.status === 409
          ? "이미 분석 중이거나 사건이 등록된 회차입니다. " +
              "다시 분석하려면 '기존 사건 교체'를 선택해 주세요."
          : err.message,
      );
    } finally {
      setBusy(false);
      setOperation("");
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
    setOperation("delete");
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
      setOperation("");
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
      {uploading && (
        <OperationLoading
          title="영상을 업로드하고 있어요"
          description={`${uploadState.filename} · 다른 회차로 이동해 추가 영상을 업로드할 수 있습니다.`}
        />
      )}

      {uploadState?.status === "failed" && (
        <p
          className="admin-video__alert admin-video__alert--error"
          role="alert"
        >
          {uploadState.filename} · {uploadState.error}
        </p>
      )}

      {operationBusy && (
        <OperationLoading
          title={
            operation === "upload"
              ? "영상을 업로드하고 있어요"
              : operation === "analysis"
                ? "영상 분석 요청을 전달하고 있어요"
                : "영상을 삭제하고 있어요"
          }
          description={
            operation === "upload"
              ? `${file?.name ?? "선택한 영상"} · 서버 응답을 기다리고 있습니다. 완료될 때까지 이 화면을 유지해 주세요.`
              : operation === "analysis"
                ? "요청 접수 후에도 실제 분석은 계속 진행됩니다."
                : "서버에서 파일을 정리하고 있습니다."
          }
        />
      )}

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
          <FileUploadStatus
            completed
            label="영상"
            filename={video.filename}
            sizeBytes={video.sizeBytes}
          />

          <div className="admin-video__actions">
            <button
              type="button"
              className="admin-video__submit"
              disabled={busy}
              onClick={requestAnalysis}
            >
              {busy ? "처리 중..." : "영상만으로 분석 (음성 인식)"}
            </button>

            <label className="admin-video__replace">
              <input
                type="checkbox"
                checked={replace}
                disabled={busy}
                onChange={(event) => setReplace(event.target.checked)}
              />
              기존 사건 교체
            </label>

            <button
              type="button"
              className="admin-video__delete"
              disabled={busy}
              onClick={deleteVideo}
            >
              {busy ? "처리 중..." : "영상 삭제"}
            </button>
          </div>
        </div>
      ) : (
        <form className="admin-video__upload-form" onSubmit={uploadVideo}>
          <div className="admin-video__upload-area">
            <FileDropInput
              file={file}
              inputRef={fileInputRef}
              accept=".mp4,video/mp4"
              extension=".mp4"
              maxBytes={MAX_VIDEO_SIZE}
              description="MP4 영상 · 최대 400MB · 선택 후 영상 업로드를 눌러 주세요."
              disabled={busy}
              onSelect={(selected) => {
                setError("");
                setMessage("");
                setFile(selected);
              }}
              onError={(text) => {
                resetFile();
                setMessage("");
                setError(text);
              }}
            />
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
        영상 업로드 완료 후 AI 분석을 별도로 요청할 수 있습니다.
      </p>
    </section>
  );
}
