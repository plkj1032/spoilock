import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Link } from "react-router-dom";

import { followApi } from "../../api/followApi";
import { feedApi } from "../../api/feedApi";

import FollowButton from "./FollowButton";

import "../../styles/UserFollowDialog.css";

export default function UserFollowDialog({ target, currentUser, onClose }) {
  const dialogRef = useRef(null);

  // ============================================
  // 1. 팔로워 / 팔로잉 통계
  // ============================================
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // ============================================
  // 2. 사용자별 피드 미리보기
  // ============================================
  const [preview, setPreview] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(true);
  const [previewError, setPreviewError] = useState("");

  // ============================================
  // 3. 사용자 정보
  // ============================================
  const userId = target.userId;
  const nickname = target.nickname || "사용자";

  const initial = Array.from(nickname.trim() || "?")[0];

  const isMe = String(currentUser?.id) === String(userId);

  // ============================================
  // 4. 프로필 모달 열기 / 닫기
  // ============================================
  useEffect(() => {
    const dialog = dialogRef.current;
    const previousFocus = document.activeElement;

    if (!dialog.open) {
      dialog.showModal();
    }

    return () => {
      if (dialog.open) {
        dialog.close();
      }

      // 이전에 포커스되어 있던 요소로 복귀
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) {
        previousFocus.focus();
      }
    };
  }, []);

  // ============================================
  // 5. 팔로워 / 팔로잉 수 가져오기
  // ============================================
  useEffect(() => {
    let active = true;

    followApi
      .getStats(userId)
      .then((data) => {
        if (!active) return;

        setStats(data);
        setError("");
      })
      .catch((err) => {
        if (!active) return;

        setError(err.message);
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [userId]);

  // ============================================
  // 6. 작성자 피드 미리보기 가져오기
  //
  // GET /api/feed/users/{userId}/preview
  //
  // 응답:
  // {
  //   userId,
  //   postCount,
  //   recentPosts: [...]
  // }
  // ============================================
  useEffect(() => {
    let active = true;

    feedApi
      .getUserPreview(userId)
      .then((data) => {
        if (!active) return;

        setPreview(data);
        setPreviewError("");
      })
      .catch((err) => {
        if (!active) return;

        setPreviewError(err.message);
      })
      .finally(() => {
        if (active) {
          setPreviewLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [userId]);

  // ============================================
  // 7. 팔로우 상태가 변경되면 통계 갱신
  // ============================================
  function handleFollowChanged(updatedStats) {
    if (updatedStats) {
      setStats(updatedStats);
    }
  }

  // ============================================
  // 8. 프로필 화면
  // ============================================
  return createPortal(
    <dialog
      ref={dialogRef}
      className="user-follow-dialog"
      aria-label={`${nickname}님의 프로필`}
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
      onClick={(event) => {
        // 팝업 바깥 영역 클릭 시 닫기
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >
      {/* ======================================
          닫기 버튼
      ====================================== */}
      <button
        type="button"
        className="user-follow-dialog__close"
        onClick={onClose}
        aria-label="프로필 닫기"
      >
        ×
      </button>

      {/* ======================================
          사용자 프로필 상단
      ====================================== */}
      <div className="user-follow-dialog__profile">
        <div className="user-follow-dialog__avatar">{initial}</div>

        <h2>{nickname}</h2>

        <p>SPOILOCK 멤버</p>
      </div>

      {/* ======================================
          팔로워 / 팔로잉 / 게시글 통계
      ====================================== */}
      <div className="user-follow-dialog__stats">
        {/* 팔로워 수 */}
        <div className="user-follow-dialog__stat">
          <strong>{loading ? "..." : (stats?.followerCount ?? "-")}</strong>

          <span>팔로워</span>
        </div>

        {/* 팔로잉 수 */}
        <div className="user-follow-dialog__stat">
          <strong>{loading ? "..." : (stats?.followingCount ?? "-")}</strong>

          <span>팔로잉</span>
        </div>

        {/* 게시글 수 */}
        <div className="user-follow-dialog__stat">
          <strong>
            {previewLoading ? "..." : (preview?.postCount ?? "-")}
          </strong>

          <span>게시글</span>
        </div>
      </div>

      {/* ======================================
          통계 조회 오류
      ====================================== */}
      {error && (
        <p className="user-follow-dialog__error" role="alert">
          {error}
        </p>
      )}

      {/* ======================================
          팔로우 / 언팔로우 버튼
      ====================================== */}
      {!isMe && currentUser && (
        <div className="user-follow-dialog__actions">
          <FollowButton
            targetUserId={userId}
            currentUser={currentUser}
            onChanged={handleFollowChanged}
          />
        </div>
      )}

      {/* 본인 프로필인 경우 */}
      {isMe && <p className="user-follow-dialog__me">내 프로필입니다.</p>}

      {/* ======================================
          최근 피드 3개
      ====================================== */}
      <section className="user-follow-dialog__feeds">
        <div className="user-follow-dialog__section-heading">
          <h3>최근 피드</h3>

          <span>최대 3개</span>
        </div>

        {/* 피드 로딩 */}
        {previewLoading && (
          <p className="user-follow-dialog__feed-status" role="status">
            최근 피드를 불러오는 중...
          </p>
        )}

        {/* 피드 조회 오류 */}
        {previewError && (
          <p className="user-follow-dialog__error" role="alert">
            최근 피드를 불러오지 못했습니다. {previewError}
          </p>
        )}

        {/* 작성한 피드가 없는 경우 */}
        {!previewLoading &&
          !previewError &&
          (preview?.recentPosts?.length ?? 0) === 0 && (
            <div className="user-follow-dialog__feed-placeholder">
              <div className="user-follow-dialog__feed-icon">▤</div>

              <strong>아직 작성한 피드가 없어요</strong>

              <p>첫 번째 피드가 등록되면 이곳에 표시됩니다.</p>
            </div>
          )}

        {/* 최근 피드 최대 3개 표시 */}
        {!previewLoading &&
          !previewError &&
          preview?.recentPosts?.map((post) => (
            <div key={post.postId} className="user-follow-dialog__feed-preview">
              {/* 드라마 이름 */}
              <span>{post.dramaTitle}</span>

              {/* 내용 또는 스포일러 보호 표시 */}
              <p>
                {post.visible
                  ? (post.content ?? "")
                  : "🔒 스포일러 보호 중인 피드입니다."}
              </p>
            </div>
          ))}

        {/* ====================================
            해당 사용자의 모든 피드 보기
        ==================================== */}
        <Link
          className="user-follow-dialog__all-feeds"
          to={`/community/users/${userId}`}
          onClick={onClose}
        >
          모든 피드 보기 →
        </Link>
      </section>
    </dialog>,
    document.body,
  );
}
