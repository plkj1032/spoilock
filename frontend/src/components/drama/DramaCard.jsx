export default function DramaCard({ drama, onAction }) {
  return (
    <article className="drama-card">
      <div className={`drama-cover cover-${drama.cover}`}>
        <span className="demo-mark">DEMO</span>
        <span className="rating">★ {drama.rating}</span>
      </div>
      <div className="drama-description">
        <p className="eyebrow muted">{drama.genre}</p>
        <h3>{drama.title}</h3>
        <div className="progress-label">
          <span>
            {drama.watched ? `${drama.watched}화 시청` : "진도 미설정"}
          </span>
          <span>총 {drama.total}화</span>
        </div>
        <div className="progress-track" aria-label={`${drama.title} 시청 진도`}>
          <span style={{ width: `${(drama.watched / drama.total) * 100}%` }} />
        </div>
        <button type="button" className="card-action" onClick={onAction}>
          시청 진도 설정 <span aria-hidden="true">›</span>
        </button>
      </div>
    </article>
  );
}
