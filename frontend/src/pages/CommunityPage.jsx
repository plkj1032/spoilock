import { useEffect, useRef, useState } from "react";

import { dramaApi } from "../api/dramaApi";
import { feedApi } from "../api/feedApi";
import FeedCard from "../components/feed/FeedCard";

import "../styles/CommunityPage.css";
import "../styles/FeedPage.css";

const PAGE_SIZE = 10;
const MAX_IMAGES = 4;
const MAX_IMAGE_SIZE = 5 * 1024 * 1024;

// ==================================================
// 1. 피드 작성 컴포넌트
// ==================================================
function FeedComposer({ user, onLogin, onCreated, onNotice }) {
  const [dramas, setDramas] = useState([]);
  const [dramaId, setDramaId] = useState("");
  const [content, setContent] = useState("");

  const [files, setFiles] = useState([]);
  const fileInputRef = useRef(null);

  const [loadingDramas, setLoadingDramas] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  // 공개된 드라마 목록 조회
  useEffect(() => {
    let active = true;

    dramaApi
      .getDramas()
      .then((result) => {
        if (!active) return;

        const list = Array.isArray(result) ? result : [];

        setDramas(list);

        if (list.length > 0) {
          setDramaId(String(list[0].id));
        }
      })
      .catch((err) => {
        if (active) {
          setError(err.message);
        }
      })
      .finally(() => {
        if (active) {
          setLoadingDramas(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  // ================================================
  // 사진 선택 및 검증
  // ================================================
  function handleFileChange(event) {
    const selected = Array.from(event.target.files ?? []);

    if (selected.length > MAX_IMAGES) {
      setError("사진은 최대 4장까지 첨부할 수 있습니다.");
      setFiles([]);
      event.target.value = "";
      return;
    }

    const invalidType = selected.some(
      (file) => !["image/jpeg", "image/png"].includes(file.type),
    );

    if (invalidType) {
      setError("JPG 또는 PNG 이미지만 첨부할 수 있습니다.");
      setFiles([]);
      event.target.value = "";
      return;
    }

    const invalidSize = selected.some(
      (file) => file.size === 0 || file.size > MAX_IMAGE_SIZE,
    );

    if (invalidSize) {
      setError("사진은 한 장당 5MB 이하이며 빈 파일은 등록할 수 없습니다.");
      setFiles([]);
      event.target.value = "";
      return;
    }

    setError("");
    setFiles(selected);
  }

  function clearFiles() {
    setFiles([]);

    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  }

  // ================================================
  // 피드 게시
  // 1) posts 저장
  // 2) post_media 업로드
  // 3) 피드 목록 갱신
  // ================================================
  async function handleSubmit(event) {
    event.preventDefault();

    if (!user) {
      onLogin();
      return;
    }

    if (saving || !dramaId || !content.trim()) {
      return;
    }

    setSaving(true);
    setError("");

    try {
      // 먼저 게시글 생성
      const created = await feedApi.createPost(dramaId, content.trim());

      const postId = created?.postId;
      let imageError = null;

      // 사진이 있을 경우 추가 업로드
      if (files.length > 0) {
        if (!postId) {
          imageError = new Error(
            "게시글 ID를 확인할 수 없어 이미지를 첨부하지 못했습니다.",
          );
        } else {
          try {
            await feedApi.uploadMedia(postId, files);
          } catch (err) {
            imageError = err;
          }
        }
      }

      // 게시글 저장 성공 후 입력값 초기화
      setContent("");
      clearFiles();

      // 전체 피드 첫 페이지 다시 조회
      onCreated();

      if (imageError) {
        onNotice?.(
          "피드 본문은 등록되었지만 사진 업로드에는 실패했습니다. " +
            imageError.message,
        );
      } else {
        onNotice?.(
          "피드가 등록되었습니다. 스포일러 분석·검수가 완료되기 전에는 다른 사용자에게 원문이 공개되지 않습니다.",
        );
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="feed-composer">
      <div className="feed-composer__heading">
        <div className="feed-composer__avatar">
          {Array.from(user?.nickname || "S")[0]}
        </div>

        <div>
          <strong>{user?.nickname ?? "SPOILOCK 사용자"}</strong>
          <p>드라마에 대한 이야기를 남겨 보세요.</p>
        </div>
      </div>

      {!user ? (
        <button
          type="button"
          className="feed-composer__login"
          onClick={onLogin}
        >
          로그인하고 피드 작성하기
        </button>
      ) : (
        <form onSubmit={handleSubmit}>
          {/* 드라마 선택 */}
          <select
            className="feed-composer__select"
            value={dramaId}
            onChange={(event) => setDramaId(event.target.value)}
            disabled={loadingDramas || saving}
            aria-label="피드 드라마 선택"
          >
            {dramas.length === 0 && (
              <option value="">등록 가능한 드라마가 없습니다</option>
            )}

            {dramas.map((drama) => (
              <option key={drama.id} value={drama.id}>
                {drama.title}
              </option>
            ))}
          </select>

          {/* 게시글 내용 */}
          <textarea
            className="feed-composer__textarea"
            placeholder="오늘 본 드라마에 대한 생각을 나눠보세요."
            value={content}
            onChange={(event) => setContent(event.target.value)}
            maxLength={2000}
            rows={4}
            disabled={saving}
          />

          {/* 이미지 첨부 */}
          <div className="feed-composer__images">
            <label htmlFor="feed-image-input">
              <span aria-hidden="true">📷 </span>
              사진 첨부 (최대 4장)
            </label>

            <input
              ref={fileInputRef}
              id="feed-image-input"
              type="file"
              accept="image/jpeg,image/png"
              multiple
              disabled={saving}
              onChange={handleFileChange}
            />

            {files.length > 0 && (
              <div className="feed-composer__selection">
                <p>선택한 사진: {files.length}장</p>

                <ul>
                  {files.map((file, index) => (
                    <li key={`${file.name}-${index}`}>{file.name}</li>
                  ))}
                </ul>

                <button type="button" onClick={clearFiles} disabled={saving}>
                  사진 선택 취소
                </button>
              </div>
            )}
          </div>

          {/* 게시 버튼 */}
          <div className="feed-composer__bottom">
            <span>{content.length} / 2000</span>

            <button
              type="submit"
              className="feed-composer__submit"
              disabled={saving || loadingDramas || !dramaId || !content.trim()}
            >
              {saving ? "게시 중..." : "피드 게시"}
            </button>
          </div>
        </form>
      )}

      {error && (
        <p className="feed-error" role="alert">
          {error}
        </p>
      )}
    </section>
  );
}

// ==================================================
// 2. 전체 / 팔로잉 / 사용자별 피드 공통 컴포넌트
//
// export 필수!
// UserFeedPage.jsx에서 import하기 때문
// ==================================================
export function FeedList({ type = "ALL", user, authorId = null }) {
  const [items, setItems] = useState([]);

  // 현재 요청 커서
  const [cursor, setCursor] = useState(null);

  // 다음 페이지 커서
  const [nextCursor, setNextCursor] = useState(null);

  const [hasNext, setHasNext] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retryKey, setRetryKey] = useState(0);

  // 무한스크롤 감시용
  const bottomRef = useRef(null);

  // ================================================
  // 피드 목록 조회
  // ================================================
  useEffect(() => {
    let active = true;
    const controller = new AbortController();

    // 특정 사용자 피드 조회
    const request =
      authorId != null
        ? feedApi.getUserFeed(authorId, {
            cursor,
            size: PAGE_SIZE,
            signal: controller.signal,
          })
        : feedApi.getFeed({
            type,
            cursor,
            size: PAGE_SIZE,
            signal: controller.signal,
          });

    request
      .then((data) => {
        if (!active) return;

        const incoming = data.items ?? [];

        setItems((current) => {
          // 첫 페이지 조회 시 전체 교체
          if (cursor === null) {
            return incoming;
          }

          // 다음 페이지는 기존 목록에 추가
          const existingIds = new Set(current.map((post) => post.postId));

          const unique = incoming.filter(
            (post) => !existingIds.has(post.postId),
          );

          return [...current, ...unique];
        });

        const newCursor = data.nextCursor ?? null;

        setNextCursor(newCursor);

        // 다음 페이지가 있는지 확인
        setHasNext(Boolean(data.hasNext && newCursor && newCursor !== cursor));

        setError("");
      })
      .catch((err) => {
        if (!active || err.name === "AbortError") {
          return;
        }

        setError(err.message);
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [authorId, type, cursor, retryKey]);

  // ================================================
  // IntersectionObserver 무한스크롤
  // ================================================
  useEffect(() => {
    const bottom = bottomRef.current;

    if (!bottom || loading || error || !hasNext || !nextCursor) {
      return;
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting) {
          return;
        }

        observer.disconnect();

        setLoading(true);
        setCursor(nextCursor);
      },
      {
        rootMargin: "280px",
        threshold: 0,
      },
    );

    observer.observe(bottom);

    return () => {
      observer.disconnect();
    };
  }, [loading, error, hasNext, nextCursor]);

  // ================================================
  // 오류 시 현재 페이지 재요청
  // ================================================
  function retry() {
    setError("");
    setLoading(true);
    setRetryKey((current) => current + 1);
  }

  // ================================================
  // 빈 목록 문구
  // ================================================
  function getEmptyTitle() {
    if (authorId != null) {
      return "아직 작성한 피드가 없어요";
    }

    if (type === "FOLLOWING") {
      return "아직 팔로잉 피드가 없어요";
    }

    return "아직 등록된 피드가 없어요";
  }

  function getEmptyDescription() {
    if (authorId != null) {
      return "이 사용자의 첫 번째 피드를 기다리고 있어요.";
    }

    if (type === "FOLLOWING") {
      return "채팅방에서 마음에 드는 사용자를 팔로우해 보세요.";
    }

    return "첫 번째 피드를 작성해 보세요.";
  }

  return (
    <section
      className="feed-list"
      aria-label={
        authorId != null
          ? "사용자별 피드"
          : type === "FOLLOWING"
            ? "팔로잉 피드"
            : "전체 피드"
      }
    >
      {/* 피드 카드 */}
      {items.map((post) => (
        <FeedCard key={post.postId} post={post} user={user} />
      ))}

      {/* 게시글 없음 */}
      {!loading && !error && items.length === 0 && (
        <div className="feed-empty">
          <span aria-hidden="true">📭</span>

          <strong>{getEmptyTitle()}</strong>

          <p>{getEmptyDescription()}</p>
        </div>
      )}

      {/* 요청 오류 */}
      {error && (
        <div className="feed-list__error" role="alert">
          <p>피드를 불러오지 못했습니다. {error}</p>

          <button type="button" onClick={retry}>
            다시 시도
          </button>
        </div>
      )}

      {/* 로딩 */}
      {loading && (
        <p className="feed-list__loading" role="status">
          피드를 불러오는 중...
        </p>
      )}

      {/* 무한스크롤 감시 영역 */}
      <div ref={bottomRef} className="feed-list__sentinel" aria-hidden="true" />

      {/* 마지막 페이지 */}
      {!loading && !error && !hasNext && items.length > 0 && (
        <p className="feed-list__end">모든 피드를 확인했습니다.</p>
      )}
    </section>
  );
}

// ==================================================
// 3. 커뮤니티 메인 화면
// ==================================================
export default function CommunityPage({ user, onLogin, onNotice }) {
  const [tab, setTab] = useState("ALL");
  const [revision, setRevision] = useState(0);

  // 새 글 작성 후 전체 피드 처음부터 새로 조회
  function handleCreated() {
    setTab("ALL");

    setRevision((current) => current + 1);
  }

  return (
    <main className="container main-content community-page feed-page">
      {/* 페이지 헤더 */}
      <header className="feed-page__heading">
        <span className="section-overline">SPOILOCK SOCIAL</span>

        <h1>모두의 이야기</h1>

        <p>
          시청한 작품의 감상을 자유롭게 나누세요. 스포일러는 SPOILOCK이
          보호합니다.
        </p>
      </header>

      {/* 게시글 작성 */}
      <FeedComposer
        user={user}
        onLogin={onLogin}
        onCreated={handleCreated}
        onNotice={onNotice}
      />

      {/* 전체 / 팔로잉 탭 */}
      <div className="feed-page__tabs" role="group" aria-label="피드 종류">
        <button
          type="button"
          className={tab === "ALL" ? "is-active" : ""}
          aria-pressed={tab === "ALL"}
          onClick={() => setTab("ALL")}
        >
          전체 피드
        </button>

        <button
          type="button"
          className={tab === "FOLLOWING" ? "is-active" : ""}
          aria-pressed={tab === "FOLLOWING"}
          onClick={() => setTab("FOLLOWING")}
        >
          팔로잉
        </button>
      </div>

      {/* 로그인 상태에 따라 피드 표시 */}
      {user ? (
        <FeedList
          key={`${user.id}:${tab}:${revision}`}
          type={tab}
          user={user}
        />
      ) : (
        <div className="feed-empty">
          <strong>로그인 후 피드를 볼 수 있습니다.</strong>

          <p>드라마를 좋아하는 사람들의 이야기를 만나보세요.</p>

          <button type="button" onClick={onLogin}>
            로그인
          </button>
        </div>
      )}
    </main>
  );
}
