import { useEffect, useRef, useState } from "react";
import { adminVideoApi } from "../../api/adminVideoApi";

import "../../styles/AdminEpisodeUpload.css";

const MAX_SIZE = 400 * 1024 * 1024;

export default function AdminEpisodeUpload({
  episodeId,
  disabled,
  onUploadStart,
  onUploadEnd,
}) {
  const inputRef = useRef(null);
  const busyRef = useRef(false);

  const [video, setVideo] = useState(null);
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    let active = true;

    adminVideoApi
      .getVideo(episodeId)
      .then((data) => {
        if (!active) return;
        setVideo(data);
        setLoadError("");
      })
      .catch((err) => {
        if (active) {
          setLoadError(err.message || "영상 정보를 확인하지 못했습니다.");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [episodeId, revision]);

  function reload() {
    setLoading(true);
    setLoadError("");
    setRevision((value) => value + 1);
  }

  function selectFile(event) {
    const selected = event.target.files?.[0];
    setError("");
    setFile(null);

    if (!selected) return;

    if (
      !selected.name.toLowerCase().endsWith(".mp4") ||
      selected.size === 0 ||
      selected.size > MAX_SIZE
    ) {
      event.target.value = "";
      setError("400MB 이하의 MP4 파일을 선택해 주세요.");
      return;
    }

    setFile(selected);
  }

  async function upload() {
    if (!file || disabled || busyRef.current || video?.registered) return;

    busyRef.current = true;
    setUploading(true);
    setError("");
    onUploadStart();

    try {
      const result = await adminVideoApi.upload(episodeId, file);

      setVideo(result);
      setFile(null);

      if (inputRef.current) {
        inputRef.current.value = "";
      }
    } catch (err) {
      // 서버에 저장됐지만 응답만 유실됐을 가능성도 확인
      try {
        const latest = await adminVideoApi.getVideo(episodeId);
        setVideo(latest);

        if (latest.registered) {
          setFile(null);

          if (inputRef.current) {
            inputRef.current.value = "";
          }
        } else {
          setError(err.message || "업로드에 실패했습니다. 다시 시도해 주세요.");
        }
      } catch {
        setLoadError(
          "업로드 결과를 확인하지 못했습니다. 다시 확인을 눌러 주세요.",
        );
      }
    } finally {
      busyRef.current = false;
      setUploading(false);
      onUploadEnd();
    }
  }

  return (
    <div className="episode-upload">
      {loading ? (
        <p role="status">영상 정보를 확인하고 있어요…</p>
      ) : loadError ? (
        <>
          <p className="episode-upload-error" role="alert">
            {loadError}
          </p>

          <button
            type="button"
            onClick={reload}
            disabled={disabled || uploading}
          >
            다시 확인
          </button>
        </>
      ) : video?.registered ? (
        <div className="episode-upload-ready">
          <span>✓ 영상 등록 완료</span>
          <strong>{video.filename}</strong>
          <small>
            {((video.sizeBytes ?? 0) / (1024 * 1024)).toFixed(1)} MB
          </small>
        </div>
      ) : (
        <>
          <label htmlFor={`upload-episode-${episodeId}`}>
            회차 영상 · MP4 / 최대 400MB
          </label>

          <input
            ref={inputRef}
            id={`upload-episode-${episodeId}`}
            type="file"
            accept=".mp4,video/mp4"
            onChange={selectFile}
            disabled={disabled || uploading}
          />

          {file && (
            <p>
              {file.name}
              {" · "}
              {(file.size / (1024 * 1024)).toFixed(1)} MB
            </p>
          )}

          <button
            type="button"
            onClick={upload}
            disabled={!file || disabled || uploading}
          >
            {uploading ? "영상 업로드 중…" : "영상 업로드"}
          </button>

          {error && (
            <p className="episode-upload-error" role="alert">
              {error}
            </p>
          )}
        </>
      )}
    </div>
  );
}
