import { useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Link } from "react-router-dom";
import { dramaApi } from "../../api/dramaApi";
import "../../styles/DramaDetailLayout.css";
import "../../styles/DramaDetailModal.css";

export default function DramaDetailModal({ dramaId, onClose }) {
  const dialogRef = useRef(null);
  const titleId = useId();

  const [detail, setDetail] = useState(null);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
  const [posterFailed, setPosterFailed] = useState(false);

  useEffect(() => {
    const dialog = dialogRef.current;
    const previousFocus = document.activeElement;
    const previousOverflow = document.body.style.overflow;

    dialog.showModal();
    document.body.style.overflow = "hidden";

    return () => {
      dialog.close();
      document.body.style.overflow = previousOverflow;

      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) {
        previousFocus.focus();
      }
    };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    dramaApi
      .getDetail(dramaId, controller.signal)
      .then((data) => {
        if (active) setDetail(data);
      })
      .catch((err) => {
        if (active && !controller.signal.aborted) {
          setError(err.message || "작품 정보를 불러오지 못했습니다.");
        }
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [dramaId, revision]);

  function retry() {
    setError("");
    setDetail(null);
    setRevision((value) => value + 1);
  }

  let officialHomepageUrl = null;

  if (detail?.officialHomepageUrl) {
    try {
      const url = new URL(detail.officialHomepageUrl);

      if (url.protocol === "https:" || url.protocol === "http:") {
        officialHomepageUrl = url.href;
      }
    } catch {
      // 올바르지 않은 주소는 링크로 표시하지 않음
    }
  }

  return createPortal(
    <dialog
      ref={dialogRef}
      className="drama-detail-dialog"
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
    >
      <header className="drama-detail-header">
        <span>DRAMA DETAIL</span>

        <button
          type="button"
          className="drama-detail-close"
          aria-label="작품 상세 닫기"
          onClick={onClose}
          autoFocus
        >
          ×
        </button>
      </header>

      {error ? (
        <div className="drama-detail-status">
          <h2 id={titleId}>작품 정보</h2>
          <p role="alert">{error}</p>
          <button type="button" onClick={retry}>
            다시 시도
          </button>
        </div>
      ) : !detail ? (
        <div className="drama-detail-status" role="status">
          <h2 id={titleId}>작품 정보를 불러오는 중…</h2>
        </div>
      ) : (
        <>
          <div className="drama-detail-overview">
            <div className="drama-detail-poster">
              {detail.posterUrl && !posterFailed ? (
                <img
                  src={detail.posterUrl}
                  alt={`${detail.title} 포스터`}
                  onError={() => setPosterFailed(true)}
                />
              ) : (
                <div className="drama-detail-placeholder">
                  <span>SPOILOCK</span>
                  <strong>{detail.title}</strong>
                </div>
              )}
            </div>

            <div className="drama-detail-info">
              <div className="drama-detail-title-group">
                <span className="drama-detail-eyebrow">작품 소개</span>
                <h2 id={titleId}>{detail.title}</h2>
              </div>

              <dl className="drama-detail-facts">
                <div>
                  <dt>장르</dt>
                  <dd className="drama-detail-genres">
                    {detail.genres?.length
                      ? detail.genres.map((genre) => (
                          <span key={genre.code}>{genre.label}</span>
                        ))
                      : "미등록"}
                  </dd>
                </div>

                <div>
                  <dt>회차</dt>
                  <dd>
                    {detail.totalEpisodeCount != null
                      ? `총 ${detail.totalEpisodeCount}부작`
                      : "미등록"}
                  </dd>
                </div>

                <div>
                  <dt>개봉일 / 첫 방영일</dt>
                  <dd>
                    {detail.premiereDate?.replaceAll("-", ". ") || "미등록"}
                  </dd>
                </div>

                <div className="drama-detail-makers">
                  <div>
                    <dt>감독</dt>
                    <dd>{detail.directors || "미등록"}</dd>
                  </div>

                  <div>
                    <dt>작가</dt>
                    <dd>{detail.writers || "미등록"}</dd>
                  </div>
                </div>
              </dl>

              <div className="drama-detail-homepage">
                <span>공식 홈페이지</span>

                {officialHomepageUrl ? (
                  <a
                    href={officialHomepageUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    공식 홈페이지 바로가기
                    <span aria-hidden="true">↗</span>
                  </a>
                ) : (
                  <p>등록된 홈페이지가 없습니다.</p>
                )}
              </div>
            </div>
          </div>

          <section className="drama-detail-section">
            <h3>줄거리</h3>
            <p>{detail.synopsis?.trim() || "등록된 줄거리가 없습니다."}</p>
          </section>

          <section className="drama-detail-section">
            <h3>출연 배우</h3>
            <p>{detail.castNames || "등록된 배우 정보가 없습니다."}</p>
          </section>

          <footer className="drama-detail-actions">
            <Link to={`/chat?dramaId=${detail.id}`} onClick={onClose}>
              채팅방 가기 <span aria-hidden="true">→</span>
            </Link>
            <Link to={`/community?dramaId=${detail.id}`} onClick={onClose}>
              커뮤니티 가기 <span aria-hidden="true">→</span>
            </Link>
          </footer>
        </>
      )}
    </dialog>,
    document.body,
  );
}
