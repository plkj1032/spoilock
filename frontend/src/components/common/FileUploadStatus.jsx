import "../../styles/FileUploadStatus.css";

export default function FileUploadStatus({
  completed = false,
  filename,
  sizeBytes,
  label = "파일",
}) {
  const size =
    typeof sizeBytes === "number"
      ? sizeBytes < 1024 * 1024
        ? `${(sizeBytes / 1024).toFixed(1)} KB`
        : `${(sizeBytes / (1024 * 1024)).toFixed(2)} MB`
      : null;

  return (
    <div
      className={`upload-status-card ${
        completed ? "is-completed" : "is-selected"
      }`}
      role="status"
    >
      <span className="upload-status-card__icon" aria-hidden="true">
        {completed ? "✓" : "↑"}
      </span>

      <div className="upload-status-card__body">
        <div className="upload-status-card__heading">
          <strong>
            {completed ? `${label} 업로드 완료` : `${label} 선택 완료`}
          </strong>

          <span className="upload-status-card__badge">
            {completed ? "서버 저장 완료" : "업로드 필요"}
          </span>
        </div>

        <span className="upload-status-card__filename">{filename}</span>

        {size && <span className="upload-status-card__size">{size}</span>}

        <p className="upload-status-card__description">
          {completed
            ? "파일이 서버에 등록되어 있습니다. AI 분석 상태는 별도로 확인해 주세요."
            : "아직 서버 저장이 완료되지 않았습니다. 아래 업로드 버튼으로 등록해 주세요."}
        </p>
      </div>
    </div>
  );
}
