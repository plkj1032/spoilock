import { useEffect, useRef, useState } from "react";
import { dramaRatingApi } from "../../api/dramaRatingApi";
import "../../styles/DramaRatingPanel.css";
import StarRatingPicker from "./StartRatingPicker";

function RatingContent({ dramaId, user, refreshKey }) {
  const activeRef = useRef(false);
  const busyRef = useRef(false);
  const requestIdRef = useRef(0);

  const [data, setData] = useState(null);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    activeRef.current = true;

    return () => {
      activeRef.current = false;
    };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    const requestId = ++requestIdRef.current;

    async function load() {
      setLoading(true);
      setError("");

      try {
        const result = await dramaRatingApi.get(dramaId, controller.signal);

        if (controller.signal.aborted || requestId !== requestIdRef.current) {
          return;
        }

        setData(result);
        setSelected(result.myScore ?? null);
      } catch (err) {
        if (!controller.signal.aborted && requestId === requestIdRef.current) {
          setError(err.message || "별점을 불러오지 못했습니다.");
        }
      } finally {
        if (!controller.signal.aborted && requestId === requestIdRef.current) {
          setLoading(false);
        }
      }
    }

    void load();

    return () => controller.abort();
  }, [dramaId, refreshKey, revision]);

  async function submit(remove = false) {
    if (busyRef.current || loading || !data) return;

    if (!remove && (!data.canRate || selected === null)) return;
    if (remove && data.myScore == null) return;

    if (remove && !window.confirm("내 별점을 삭제할까요?")) {
      return;
    }

    busyRef.current = true;
    setBusy(true);
    setError("");
    setNotice("");

    const requestId = ++requestIdRef.current;

    try {
      const result = remove
        ? await dramaRatingApi.remove(dramaId)
        : await dramaRatingApi.save(dramaId, selected);

      if (!activeRef.current || requestId !== requestIdRef.current) {
        return;
      }

      setData(result);
      setSelected(result.myScore ?? null);
      setNotice(remove ? "내 별점을 삭제했습니다." : "별점을 저장했습니다.");
    } catch (err) {
      if (activeRef.current && requestId === requestIdRef.current) {
        setError(err.message || "별점을 처리하지 못했습니다.");
      }
    } finally {
      busyRef.current = false;

      if (activeRef.current) {
        setBusy(false);
      }
    }
  }

  const hasRating = data?.myScore != null;
  const canEdit = Boolean(user && data?.canRate);
  const unchanged = selected === data?.myScore;

  return (
    <section
      className="drama-rating"
      aria-label="드라마 전체 별점"
      aria-busy={loading || busy}
    >
      <header className="drama-rating__header">
        <div>
          <span className="drama-rating__eyebrow">SERIES RATING</span>
          <h3>이 작품, 어떠셨나요?</h3>
          <p>회차별 평가가 아닌 작품 전체에 대한 별점이에요.</p>
        </div>

        <div className="drama-rating__summary">
          <span aria-hidden="true">★</span>
          <strong>
            {data?.averageRating == null
              ? "—"
              : Number(data.averageRating).toFixed(1)}
          </strong>
          <small>
            {!data
              ? "조회 중"
              : data.ratingCount === 0
                ? "아직 평가 없음"
                : `${data.ratingCount.toLocaleString()}명 평가`}
          </small>
        </div>
      </header>

      {loading ? (
        <p className="drama-rating__hint" role="status">
          별점을 불러오는 중…
        </p>
      ) : data ? (
        <>
          <p className="drama-rating__mine">
            내 별점
            <strong>
              {hasRating
                ? `${(data.myScore / 2).toFixed(1)}점`
                : "아직 평가하지 않았어요"}
            </strong>
          </p>

          {canEdit ? (
            <StarRatingPicker
              value={selected}
              disabled={busy}
              onChange={(score) => {
                setSelected(score);
                setNotice("");
              }}
            />
          ) : (
            <p className="drama-rating__hint">
              {!user
                ? "로그인 후 1화 이상 시청 진도를 설정하면 평가할 수 있어요."
                : "1화 이상 시청 진도를 설정하면 평가할 수 있어요."}
            </p>
          )}

          <div className="drama-rating__actions">
            {canEdit && (
              <button
                type="button"
                className="drama-rating__save"
                disabled={busy || selected === null || unchanged}
                onClick={() => submit(false)}
              >
                {busy ? "처리 중…" : hasRating ? "별점 수정" : "별점 저장"}
              </button>
            )}

            {user && hasRating && (
              <button
                type="button"
                disabled={busy}
                onClick={() => submit(true)}
              >
                내 별점 삭제
              </button>
            )}
          </div>
        </>
      ) : null}

      {notice && (
        <p className="drama-rating__success" role="status">
          {notice}
        </p>
      )}

      {error && (
        <div className="drama-rating__error" role="alert">
          <span>{error}</span>
          <button
            type="button"
            disabled={busy || loading}
            onClick={() => setRevision((value) => value + 1)}
          >
            다시 조회
          </button>
        </div>
      )}
    </section>
  );
}

export default function DramaRatingPanel({ dramaId, user, refreshKey = 0 }) {
  return (
    <RatingContent
      key={`${dramaId}:${user?.id ?? "guest"}`}
      dramaId={dramaId}
      user={user}
      refreshKey={refreshKey}
    />
  );
}
