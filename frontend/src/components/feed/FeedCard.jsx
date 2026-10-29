import { useEffect, useState } from "react";

import { feedApi } from "../../api/feedApi";
import FollowButton from "../common/FollowButton";
import UserFollowDialog from "../user/UserFollowDialog";

const BLOCK_MESSAGES = {
  ANALYZING: "AI가 스포일러 여부를 확인하고 있어요.",
  REVIEW: "스포일러 검토가 필요하여 원문을 보호하고 있어요.",
  NOT_WATCHED: "아직 시청하지 않은 회차의 내용이 포함되어 있어요.",
};

function formatDate(value) {
  if (!value) return "";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) return "";

  return date.toLocaleString("ko-KR", {
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function FeedCard({ post, user }) {
  const [profileTarget, setProfileTarget] = useState(null);

  const [social, setSocial] = useState(null);
  const [media, setMedia] = useState([]);
  const [mediaIndex, setMediaIndex] = useState(0);

  const [commentsOpen, setCommentsOpen] = useState(false);
  const [comments, setComments] = useState([]);
  const [commentsPage, setCommentsPage] = useState(0);
  const [hasMoreComments, setHasMoreComments] = useState(false);
  const [commentText, setCommentText] = useState("");

  const [busy, setBusy] = useState(false);
  const [loadingComments, setLoadingComments] = useState(false);
  const [error, setError] = useState("");

  const mine = String(post.authorId) === String(user.id);

  // 게시글에 접근 가능한 경우에만 부가정보 조회
  useEffect(() => {
    if (!post.visible) return;

    let active = true;

    Promise.all([feedApi.getSocial(post.postId), feedApi.getMedia(post.postId)])
      .then(([stats, images]) => {
        if (!active) return;

        setSocial(stats);
        setMedia(images);
      })
      .catch((err) => {
        if (active) setError(err.message);
      });

    return () => {
      active = false;
    };
  }, [post.postId, post.visible]);

  // 댓글창을 닫아둬도 좋아요·댓글 숫자는 자동 갱신
  useEffect(() => {
    if (!post.visible || busy) return;

    let active = true;
    let refreshing = false;
    const controller = new AbortController();

    async function refreshSocial() {
      if (!active || refreshing || document.visibilityState !== "visible") {
        return;
      }

      refreshing = true;

      try {
        const stats = await feedApi.getSocial(post.postId, controller.signal);

        if (active) {
          setSocial(stats);
        }
      } catch {
        // 자동 조회 실패 시 기존 숫자 유지
      } finally {
        refreshing = false;
      }
    }

    const timer = window.setInterval(refreshSocial, 5000);
    document.addEventListener("visibilitychange", refreshSocial);
    window.addEventListener("focus", refreshSocial);

    return () => {
      active = false;
      controller.abort();
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", refreshSocial);
      window.removeEventListener("focus", refreshSocial);
    };
  }, [post.postId, post.visible, busy]);

  // 댓글 내용은 댓글창을 펼쳐둔 경우에만 자동 갱신
  useEffect(() => {
    if (!post.visible || !commentsOpen || busy || loadingComments) {
      return;
    }

    let active = true;
    let refreshing = false;
    const controller = new AbortController();

    async function refreshComments() {
      if (!active || refreshing || document.visibilityState !== "visible") {
        return;
      }

      refreshing = true;

      try {
        const pages = await Promise.all(
          Array.from({ length: commentsPage + 1 }, (_, page) =>
            feedApi.getComments(post.postId, page, controller.signal),
          ),
        );

        if (!active) return;

        const merged = new Map();

        for (const page of pages) {
          for (const comment of page.content ?? []) {
            merged.set(comment.id, comment);
          }
        }

        setComments([...merged.values()]);
        setHasMoreComments(!pages[pages.length - 1].last);
      } catch {
        // 자동 조회 실패 시 기존 댓글 유지
      } finally {
        refreshing = false;
      }
    }

    const timer = window.setInterval(refreshComments, 5000);
    document.addEventListener("visibilitychange", refreshComments);
    window.addEventListener("focus", refreshComments);

    return () => {
      active = false;
      controller.abort();
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", refreshComments);
      window.removeEventListener("focus", refreshComments);
    };
  }, [
    post.postId,
    post.visible,
    commentsOpen,
    commentsPage,
    busy,
    loadingComments,
  ]);

  function openProfile() {
    setProfileTarget({
      userId: post.authorId,
      nickname: post.authorNickname,
    });
  }

  async function toggleLike() {
    if (busy || !social) return;

    setBusy(true);
    setError("");

    try {
      const updated = social.liked
        ? await feedApi.unlike(post.postId)
        : await feedApi.like(post.postId);

      setSocial(updated);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function loadComments(page = 0) {
    if (loadingComments) return;

    setLoadingComments(true);
    setError("");

    try {
      const data = await feedApi.getComments(post.postId, page);

      setComments((current) => {
        if (page === 0) return data.content ?? [];

        const existing = new Set(current.map((comment) => comment.id));

        const incoming = (data.content ?? []).filter(
          (comment) => !existing.has(comment.id),
        );

        return [...current, ...incoming];
      });

      setCommentsPage(page);
      setHasMoreComments(!data.last);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoadingComments(false);
    }
  }

  function toggleComments() {
    if (commentsOpen) {
      setCommentsOpen(false);
      return;
    }

    setCommentsOpen(true);
    void loadComments(0);
  }

  async function submitComment(event) {
    event.preventDefault();

    const text = commentText.trim();

    if (!text || text.length > 1000 || busy) return;

    setBusy(true);
    setError("");

    try {
      await feedApi.createComment(post.postId, text);

      setCommentText("");

      // 최신 댓글 및 댓글 수 동기화
      const [data, stats] = await Promise.all([
        feedApi.getComments(post.postId, 0),
        feedApi.getSocial(post.postId),
      ]);

      setComments(data.content ?? []);
      setCommentsPage(0);
      setHasMoreComments(!data.last);
      setSocial(stats);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <article className="feed-card">
      <header className="feed-card__header">
        <button
          type="button"
          className="feed-card__author"
          onClick={openProfile}
          aria-label={`${post.authorNickname}님의 프로필 보기`}
        >
          <div className="feed-card__avatar">
            {Array.from(post.authorNickname || "?")[0]}
          </div>

          <div className="feed-card__author-info">
            <strong>{post.authorNickname}</strong>

            <span>
              {post.dramaTitle} · {formatDate(post.createdAt)}
            </span>
          </div>
        </button>

        {!mine && (
          <div className="feed-card__follow">
            <FollowButton targetUserId={post.authorId} currentUser={user} />
          </div>
        )}
      </header>

      <div className="feed-card__body">
        {post.visible ? (
          <p className="feed-card__content">{post.content}</p>
        ) : (
          <div className="feed-card__protected">
            <span className="feed-card__shield">🔒</span>

            <strong>스포일러 보호 중</strong>

            <p>
              {BLOCK_MESSAGES[post.blockReason] ??
                "이 피드는 현재 공개할 수 없습니다."}
            </p>
          </div>
        )}
      </div>

      {/* 최대 4장의 사진 캐러셀 */}
      {post.visible && media.length > 0 && (
        <div className="feed-card__media">
          <img
            src={media[mediaIndex].url}
            alt={`${post.authorNickname}님의 첨부 이미지 ${mediaIndex + 1}`}
            loading="lazy"
          />

          {media.length > 1 && (
            <>
              <button
                type="button"
                className="feed-card__media-prev"
                onClick={() =>
                  setMediaIndex(
                    (index) => (index - 1 + media.length) % media.length,
                  )
                }
                aria-label="이전 이미지"
              >
                ‹
              </button>

              <button
                type="button"
                className="feed-card__media-next"
                onClick={() =>
                  setMediaIndex((index) => (index + 1) % media.length)
                }
                aria-label="다음 이미지"
              >
                ›
              </button>

              <span className="feed-card__media-count">
                {mediaIndex + 1} / {media.length}
              </span>
            </>
          )}
        </div>
      )}

      {/* 좋아요 / 댓글 */}
      <footer className="feed-card__footer">
        {post.visible ? (
          <>
            <button
              type="button"
              className={`feed-card__action ${social?.liked ? "is-liked" : ""}`}
              onClick={toggleLike}
              disabled={busy || !social}
              aria-pressed={social?.liked ?? false}
            >
              {social?.liked ? "♥" : "♡"} 좋아요 {social?.likeCount ?? 0}
            </button>

            <button
              type="button"
              className="feed-card__action"
              onClick={toggleComments}
              aria-expanded={commentsOpen}
            >
              ♧ 댓글 {social?.commentCount ?? 0}
            </button>
          </>
        ) : (
          <span>스포일러 보호가 적용된 피드입니다.</span>
        )}

        {mine && post.analysisStatus !== "COMPLETED" && (
          <span className="feed-card__pending">분석 대기</span>
        )}
      </footer>

      {/* 댓글 목록 및 입력 */}
      {post.visible && commentsOpen && (
        <section className="feed-card__comments">
          <h3>댓글</h3>

          {comments.map((comment) => (
            <div className="feed-card__comment" key={comment.id}>
              <strong>{comment.nickname}</strong>

              <p>
                {comment.visible ? comment.content : "🔒 분석 중인 댓글입니다."}
              </p>
            </div>
          ))}

          {!loadingComments && comments.length === 0 && (
            <p className="feed-card__comment-empty">
              첫 번째 댓글을 남겨보세요.
            </p>
          )}

          {hasMoreComments && (
            <button
              type="button"
              className="feed-card__more"
              disabled={loadingComments}
              onClick={() => loadComments(commentsPage + 1)}
            >
              이전 댓글 더 보기
            </button>
          )}

          <form className="feed-card__comment-form" onSubmit={submitComment}>
            <input
              type="text"
              value={commentText}
              onChange={(event) => setCommentText(event.target.value)}
              placeholder="댓글을 입력하세요"
              maxLength={1000}
              disabled={busy}
            />

            <button type="submit" disabled={busy || !commentText.trim()}>
              등록
            </button>
          </form>
        </section>
      )}

      {loadingComments && (
        <p className="feed-card__status">댓글을 불러오는 중...</p>
      )}

      {error && (
        <p className="feed-error" role="alert">
          {error}
        </p>
      )}

      {/* 작성자 프로필 */}
      {profileTarget && (
        <UserFollowDialog
          key={profileTarget.userId}
          target={profileTarget}
          currentUser={user}
          onClose={() => setProfileTarget(null)}
        />
      )}
    </article>
  );
}
