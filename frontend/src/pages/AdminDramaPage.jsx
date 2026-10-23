import { useEffect, useState } from "react";
import { adminDramaApi } from "../api/adminDramaApi";

const pioneerEpisodes = Array.from({ length: 6 }, (_, i) => ({
  episodeNumber: i + 1,
  title: `${i + 1}화`,
}));

export default function AdminDramaPage() {
  const [editingId, setEditingId] = useState(null);

  const [title, setTitle] = useState("Pioneer One");
  const [synopsis, setSynopsis] = useState("");
  const [posterUrl, setPosterUrl] = useState("");
  const [episodes, setEpisodes] = useState(pioneerEpisodes);

  const [dramas, setDramas] = useState([]);
  const [busy, setBusy] = useState(false);
  const [processingId, setProcessingId] = useState(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    let active = true;

    adminDramaApi
      .getAll()
      .then((data) => {
        if (active) setDramas(data);
      })
      .catch((err) => {
        if (active) setError(err.message);
      });

    return () => {
      active = false;
    };
  }, []);

  function clearMessages() {
    setError("");
    setMessage("");
  }

  function resetForm() {
    setEditingId(null);
    setTitle("");
    setSynopsis("");
    setPosterUrl("");
    setEpisodes([{ episodeNumber: 1, title: "1화" }]);
  }

  function changeEpisode(index, value) {
    setEpisodes((prev) =>
      prev.map((episode, i) =>
        i === index ? { ...episode, title: value } : episode,
      ),
    );
  }

  function addEpisode() {
    setEpisodes((prev) => [
      ...prev,
      {
        episodeNumber: prev.length + 1,
        title: `${prev.length + 1}화`,
      },
    ]);
  }

  function removeEpisode(index) {
    setEpisodes((prev) => {
      if (prev.length <= 1) return prev;

      return prev
        .filter((_, i) => i !== index)
        .map((episode, i) => ({
          ...episode,
          episodeNumber: i + 1,
          title:
            episode.title === `${episode.episodeNumber}화`
              ? `${i + 1}화`
              : episode.title,
        }));
    });
  }

  // 기존 초안을 불러와 편집
  async function startEditing(id) {
    clearMessages();
    setBusy(true);

    try {
      const detail = await adminDramaApi.getDetail(id);

      if (detail.status !== "DRAFT") {
        throw new Error("초안 상태의 작품만 수정할 수 있습니다.");
      }

      setEditingId(detail.id);
      setTitle(detail.title);
      setSynopsis(detail.synopsis ?? "");
      setPosterUrl(detail.posterUrl ?? "");

      setEpisodes(
        detail.episodes
          .sort((a, b) => a.episodeNumber - b.episodeNumber)
          .map((episode) => ({
            episodeNumber: episode.episodeNumber,
            title: episode.title,
          })),
      );

      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  // 작품 등록 또는 초안 수정
  async function handleSubmit(event) {
    event.preventDefault();
    if (busy) return;

    clearMessages();

    const payload = {
      title: title.trim(),
      synopsis: synopsis.trim(),
      posterUrl: posterUrl.trim() || null,
      totalEpisodeCount: episodes.length,
      episodes: episodes.map((episode, index) => ({
        episodeNumber: index + 1,
        title: episode.title.trim() || `${index + 1}화`,
      })),
    };

    if (!payload.title) {
      setError("작품명을 입력해주세요.");
      return;
    }

    setBusy(true);

    try {
      const saved =
        editingId === null
          ? await adminDramaApi.create(payload)
          : await adminDramaApi.update(editingId, payload);

      setDramas((prev) => {
        const withoutSaved = prev.filter((drama) => drama.id !== saved.id);

        return [saved, ...withoutSaved];
      });

      const wasEditing = editingId !== null;

      resetForm();

      setMessage(
        wasEditing
          ? `${saved.title} 작품을 수정했습니다.`
          : `${saved.title} 작품을 초안으로 등록했습니다.`,
      );
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  // 공개 / 재공개
  async function publishDrama(id) {
    if (
      !window.confirm(
        "작품을 공개하시겠습니까?\n\n" +
          "회차 정보와 저작권·공개 내용을 검토했는지 확인해 주세요.",
      )
    ) {
      return;
    }

    clearMessages();
    setProcessingId(id);

    try {
      const updated = await adminDramaApi.publish(id);

      setDramas((prev) =>
        prev.map((drama) => (drama.id === id ? updated : drama)),
      );

      setMessage(`${updated.title} 작품을 공개했습니다.`);
    } catch (err) {
      setError(err.message);
    } finally {
      setProcessingId(null);
    }
  }

  // 공개 작품 숨기기
  async function hideDrama(id) {
    if (
      !window.confirm(
        "작품을 숨기시겠습니까?\n" + "일반 드라마 목록에서 표시되지 않습니다.",
      )
    ) {
      return;
    }

    clearMessages();
    setProcessingId(id);

    try {
      const updated = await adminDramaApi.hide(id);

      setDramas((prev) =>
        prev.map((drama) => (drama.id === id ? updated : drama)),
      );

      setMessage(`${updated.title} 작품을 숨겼습니다.`);
    } catch (err) {
      setError(err.message);
    } finally {
      setProcessingId(null);
    }
  }

  return (
    <main className="container main-content admin-page">
      <div className="section-top">
        <div>
          <span className="section-overline">SPOILOCK ADMIN</span>
          <h1>콘텐츠 관리 센터</h1>
          <p className="page-subtitle">드라마와 회차 정보를 관리합니다.</p>
        </div>
      </div>

      <form className="admin-panel" onSubmit={handleSubmit}>
        <div className="admin-heading">
          <h2>
            {editingId === null ? "새 작품 등록" : `작품 #${editingId} 수정`}
          </h2>

          {editingId !== null && (
            <button
              type="button"
              className="button button-outline"
              onClick={() => {
                resetForm();
                clearMessages();
              }}
              disabled={busy}
            >
              수정 취소
            </button>
          )}
        </div>

        <div className="admin-field">
          <label htmlFor="drama-title">작품명</label>
          <input
            id="drama-title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            maxLength={200}
            required
          />
        </div>

        <div className="admin-field">
          <label htmlFor="drama-synopsis">공개용 짧은 줄거리</label>
          <textarea
            id="drama-synopsis"
            rows={4}
            maxLength={5000}
            value={synopsis}
            onChange={(e) => setSynopsis(e.target.value)}
            placeholder="중요한 반전 없이 작품의 기본 설정만 소개하세요."
          />
        </div>

        <div className="admin-field">
          <label htmlFor="drama-poster">포스터 URL (선택)</label>
          <input
            id="drama-poster"
            type="url"
            maxLength={1000}
            value={posterUrl}
            onChange={(e) => setPosterUrl(e.target.value)}
            placeholder="https://..."
          />
          <small>
            이미지 사용권한을 확인한 경우에만 입력하세요. 비워두면 기본 카드
            이미지가 표시됩니다.
          </small>
        </div>

        <div className="admin-heading">
          <h3>회차 정보 · 총 {episodes.length}화</h3>
        </div>

        <div className="admin-episode-grid">
          {episodes.map((episode, index) => (
            <div className="admin-field" key={episode.episodeNumber}>
              <label htmlFor={`episode-${index}`}>{index + 1}화 제목</label>

              <div className="admin-episode-row">
                <input
                  id={`episode-${index}`}
                  value={episode.title}
                  onChange={(e) => changeEpisode(index, e.target.value)}
                  maxLength={200}
                  placeholder={`${index + 1}화`}
                />

                <button
                  type="button"
                  className="button button-outline episode-delete"
                  onClick={() => removeEpisode(index)}
                  disabled={episodes.length === 1 || busy}
                >
                  삭제
                </button>
              </div>
            </div>
          ))}
        </div>

        <button
          type="button"
          className="button button-outline episode-add"
          onClick={addEpisode}
          disabled={busy}
        >
          + 회차 추가
        </button>

        <p className="admin-rights-note">
          작품 소개와 이미지의 이용 권한을 확인해 주세요. 자막 파일 등록 및 AI
          분석은 별도 관리자 기능으로 연결할 예정입니다.
        </p>

        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}

        {message && (
          <p className="form-success" role="status">
            {message}
          </p>
        )}

        <button
          type="submit"
          className="button button-primary admin-submit"
          disabled={busy}
        >
          {busy
            ? "저장 중..."
            : editingId === null
              ? `작품 및 ${episodes.length}개 회차 등록`
              : "작품 수정사항 저장"}
        </button>
      </form>

      <section className="admin-panel">
        <h2>등록된 작품</h2>

        {dramas.length === 0 ? (
          <p className="page-subtitle">등록된 작품이 없습니다.</p>
        ) : (
          <div className="admin-drama-list">
            {dramas.map((drama) => (
              <div className="admin-drama-item" key={drama.id}>
                <div>
                  <strong>{drama.title}</strong>
                  <p className="page-subtitle">
                    #{drama.id} · 총 {drama.totalEpisodeCount}화
                  </p>
                  <span className="admin-status">
                    {drama.status === "DRAFT"
                      ? "초안"
                      : drama.status === "PUBLISHED"
                        ? "공개"
                        : "숨김"}
                  </span>
                </div>

                <div className="admin-item-actions">
                  {drama.status === "DRAFT" && (
                    <button
                      type="button"
                      className="button button-outline"
                      onClick={() => startEditing(drama.id)}
                      disabled={busy || processingId !== null}
                    >
                      수정
                    </button>
                  )}

                  {drama.status !== "PUBLISHED" ? (
                    <button
                      type="button"
                      className="button button-primary"
                      onClick={() => publishDrama(drama.id)}
                      disabled={busy || processingId !== null}
                    >
                      {processingId === drama.id ? "처리 중..." : "공개"}
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="button button-outline"
                      onClick={() => hideDrama(drama.id)}
                      disabled={busy || processingId !== null}
                    >
                      숨기기
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}
