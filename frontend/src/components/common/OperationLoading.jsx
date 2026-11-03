import "../../styles/OperationLoading.css";

export default function OperationLoading({
  title,
  description,
  progress = null,
}) {
  const percent =
    typeof progress === "number" && Number.isFinite(progress)
      ? Math.max(0, Math.min(100, progress))
      : null;

  return (
    <div className="operation-loading">
      <span className="operation-loading__spinner" aria-hidden="true" />

      <div className="operation-loading__body">
        <strong className="operation-loading__title" role="status">
          {title}
        </strong>

        {description && (
          <p className="operation-loading__description">{description}</p>
        )}

        {percent !== null && (
          <div className="operation-loading__progress">
            <progress
              max="100"
              value={percent}
              aria-label="서버가 보고한 분석 진행률"
            />
            <span>{percent}%</span>
          </div>
        )}
      </div>
    </div>
  );
}
