import "../../styles/DramaCard.css";

export default function DramaCard({
  drama,
  watched,
  loggedIn,
  progressLoading,
  onAction,
  selected = false,
  onSelect,
}) {
  const total = drama.totalEpisodeCount ?? 0;

  const progress =
    total > 0 ? Math.min(100, ((watched ?? 0) / total) * 100) : 0;

  const progressText = !loggedIn
    ? "로그인 후 진도 확인"
    : progressLoading
      ? "진도 불러오는 중..."
      : watched == null
        ? "진도 조회 실패"
        : watched === 0
          ? "진도 미설정"
          : `${watched}화 시청`;

  return (
    <article className="drama-card" onClick={onSelect}>
      <div className="drama-cover cover-night">
        {drama.posterUrl && (
          <img
            className="drama-poster"
            src={drama.posterUrl}
            alt=""
            loading="lazy"
          />
        )}

        <span className="demo-mark">SPOILOCK</span>
      </div>

      <div className="drama-description">
        <p className="eyebrow muted">DRAMA</p>

        <h3>{drama.title}</h3>

        <div className="progress-label">
          <span>{progressText}</span>
          <span>총 {total}화</span>
        </div>

        <div className="progress-track" aria-label={`${drama.title} 시청 진도`}>
          <span style={{ width: `${progress}%` }} />
        </div>

        {onSelect && (
          <button
            type="button"
            className="card-action"
            aria-pressed={selected}
            onClick={(event) => {
              event.stopPropagation();
              onSelect();
            }}
          >
            {selected ? "선택된 드라마의 채팅방" : "이 드라마 채팅방 보기"}
            <span aria-hidden="true">›</span>
          </button>
        )}

        <button
          type="button"
          className="card-action"
          onClick={(event) => {
            event.stopPropagation();
            onAction();
          }}
        >
          시청 진도 설정
          <span aria-hidden="true">›</span>
        </button>
      </div>
    </article>
  );
}
