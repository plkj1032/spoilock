import "../../styles/AdminDramaMetadata.css";

const GENRES = [
  ["ROMANCE", "로맨스"],
  ["ROMANTIC_COMEDY", "로맨틱 코미디"],
  ["COMEDY", "코미디"],
  ["THRILLER", "스릴러"],
  ["MYSTERY", "미스터리"],
  ["CRIME", "범죄"],
  ["ACTION", "액션"],
  ["FANTASY", "판타지"],
  ["SCI_FI", "SF"],
  ["HISTORICAL", "사극"],
  ["FAMILY", "가족"],
  ["HUMAN", "휴먼"],
  ["MEDICAL", "의학"],
  ["LEGAL", "법정"],
  ["SCHOOL", "학원"],
  ["HORROR", "공포"],
  ["OTHER", "기타"],
];

export default function AdminDramaMetadataFields({
  value,
  onChange,
  disabled = false,
}) {
  function updateField(name, nextValue) {
    onChange((prev) => ({
      ...prev,
      [name]: nextValue,
    }));
  }

  function toggleGenre(genre) {
    onChange((prev) => ({
      ...prev,
      genres: prev.genres.includes(genre)
        ? prev.genres.filter((item) => item !== genre)
        : [...prev.genres, genre],
    }));
  }

  return (
    <fieldset className="admin-drama-metadata" disabled={disabled}>
      <legend>작품 상세 정보</legend>

      <fieldset className="admin-drama-genres">
        <legend>장르 · 여러 개 선택 가능</legend>

        <div className="admin-drama-genre-list">
          {GENRES.map(([code, label]) => (
            <button
              key={code}
              type="button"
              className="admin-drama-genre-chip"
              aria-pressed={value.genres.includes(code)}
              onClick={() => toggleGenre(code)}
            >
              {label}
            </button>
          ))}
        </div>
      </fieldset>

      <div className="admin-drama-metadata-grid">
        <div className="admin-field">
          <label htmlFor="drama-premiere-date">개봉일 / 첫 방영일</label>
          <input
            id="drama-premiere-date"
            type="date"
            value={value.premiereDate}
            onChange={(event) =>
              updateField("premiereDate", event.target.value)
            }
          />
        </div>

        <div className="admin-field">
          <label htmlFor="drama-directors">감독</label>
          <input
            id="drama-directors"
            value={value.directors}
            maxLength={500}
            placeholder="여러 명이면 쉼표로 구분"
            onChange={(event) => updateField("directors", event.target.value)}
          />
        </div>

        <div className="admin-field">
          <label htmlFor="drama-writers">작가</label>
          <input
            id="drama-writers"
            value={value.writers}
            maxLength={500}
            placeholder="여러 명이면 쉼표로 구분"
            onChange={(event) => updateField("writers", event.target.value)}
          />
        </div>

        <div className="admin-field">
          <label htmlFor="drama-cast-names">출연 배우</label>
          <input
            id="drama-cast-names"
            value={value.castNames}
            maxLength={2000}
            placeholder="배우 이름을 쉼표로 구분"
            onChange={(event) => updateField("castNames", event.target.value)}
          />
        </div>
        <div className="admin-field">
          <label htmlFor="drama-official-homepage">공식 홈페이지</label>
          <input
            id="drama-official-homepage"
            type="url"
            maxLength={1000}
            value={value.officialHomepageUrl}
            placeholder="https://..."
            onChange={(event) =>
              updateField("officialHomepageUrl", event.target.value)
            }
          />
        </div>
      </div>
    </fieldset>
  );
}
