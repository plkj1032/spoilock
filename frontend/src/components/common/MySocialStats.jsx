import { useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Link } from "react-router-dom";

import { followApi } from "../../api/followApi";
import { feedApi } from "../../api/feedApi";
import FollowButton from "./FollowButton";

import "../../styles/MySocialStats.css";

const TITLES = {
  posts: "내 게시글",
  followers: "팔로워",
  following: "팔로잉",
};

function ListPage({
  type,
  currentUser,
  page,
  cursor,
  onPrevious,
  onNext,
  onChanged,
  onRetry,
  onClose,
}) {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    const controller = new AbortController();

    async function load() {
      try {
        let result;

        if (type === "posts") {
          result = await feedApi.getUserFeed(currentUser.id, {
            cursor,
            size: 5,
            signal: controller.signal,
          });
        } else if (type === "followers") {
          result = await followApi.getFollowers(currentUser.id, page, 5);
        } else {
          result = await followApi.getFollowing(currentUser.id, page, 5);
        }

        if (active) setData(result);
      } catch (err) {
        if (active && err.name !== "AbortError") {
          setError(err.message);
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();

    return () => {
      active = false;
      controller.abort();
    };
  }, [type, currentUser.id, page, cursor]);

  if (loading) {
    return (
      <p className="my-social-status" role="status">
        불러오는 중…
      </p>
    );
  }

  if (error) {
    return (
      <div className="my-social-error" role="alert">
        <p>{error}</p>
        <button type="button" onClick={onRetry}>
          다시 불러오기
        </button>
      </div>
    );
  }

  const items = type === "posts" ? (data?.items ?? []) : (data?.content ?? []);

  const hasNext =
    type === "posts"
      ? Boolean(data?.hasNext && data?.nextCursor)
      : page + 1 < (data?.totalPages ?? 0);

  return (
    <>
      {items.length === 0 ? (
        <p className="my-social-status">
          {type === "posts"
            ? "아직 작성한 게시글이 없습니다."
            : type === "followers"
              ? "아직 팔로워가 없습니다."
              : "아직 팔로우한 사용자가 없습니다."}
        </p>
      ) : type === "posts" ? (
        <ul className="my-social-posts">
          {items.map((post) => (
            <li key={post.postId}>
              <span className="my-social-drama">{post.dramaTitle}</span>
              <p>
                {post.visible
                  ? post.content || "내용 없는 게시글"
                  : "🔒 스포일러 보호 중인 게시글입니다."}
              </p>
              <time dateTime={post.createdAt}>
                {new Date(post.createdAt).toLocaleString("ko-KR")}
              </time>
            </li>
          ))}
        </ul>
      ) : (
        <ul className="my-social-users">
          {items.map((person) => (
            <li key={person.userId}>
              <span className="my-social-avatar" aria-hidden="true">
                {Array.from(person.nickname?.trim() || "?")[0]}
              </span>

              <div className="my-social-person">
                <strong>{person.nickname}</strong>
                <Link
                  to={`/community/users/${person.userId}`}
                  onClick={onClose}
                >
                  작성한 피드 보기 →
                </Link>
              </div>

              <FollowButton
                targetUserId={person.userId}
                currentUser={currentUser}
                onChanged={onChanged}
              />
            </li>
          ))}
        </ul>
      )}

      {(page > 0 || hasNext) && (
        <nav className="my-social-pager" aria-label="목록 페이지">
          <button type="button" disabled={page === 0} onClick={onPrevious}>
            이전
          </button>
          <span>{page + 1}페이지</span>
          <button
            type="button"
            disabled={!hasNext}
            onClick={() => onNext(data?.nextCursor ?? null)}
          >
            다음
          </button>
        </nav>
      )}

      {type === "posts" && (
        <Link
          className="my-social-all-posts"
          to={`/community/users/${currentUser.id}`}
          onClick={onClose}
        >
          내 피드 페이지로 이동 →
        </Link>
      )}
    </>
  );
}

function SocialPanel({ type, currentUser, onClose, onChanged }) {
  const dialogRef = useRef(null);
  const titleId = useId();

  const [page, setPage] = useState(0);
  const [cursors, setCursors] = useState([null]);
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    const dialog = dialogRef.current;
    const previousFocus = document.activeElement;

    dialog.showModal();

    return () => {
      dialog.close();

      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) {
        previousFocus.focus();
      }
    };
  }, []);

  function reload() {
    setPage(0);
    setCursors([null]);
    setRevision((value) => value + 1);
  }

  return createPortal(
    <dialog
      ref={dialogRef}
      className="my-social-dialog"
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
    >
      <header className="my-social-dialog-header">
        <div>
          <span>MY SPOILOCK</span>
          <h2 id={titleId}>{TITLES[type]}</h2>
        </div>
        <button
          type="button"
          className="my-social-close"
          aria-label="목록 닫기"
          onClick={onClose}
          autoFocus
        >
          ×
        </button>
      </header>

      <div className="my-social-dialog-body">
        <ListPage
          key={`${type}:${page}:${revision}`}
          type={type}
          currentUser={currentUser}
          page={page}
          cursor={cursors[page] ?? null}
          onPrevious={() => setPage((value) => Math.max(0, value - 1))}
          onNext={(nextCursor) => {
            setCursors((previous) => [
              ...previous.slice(0, page + 1),
              nextCursor,
            ]);
            setPage((value) => value + 1);
          }}
          onChanged={() => {
            reload();
            onChanged();
          }}
          onRetry={reload}
          onClose={onClose}
        />
      </div>
    </dialog>,
    document.body,
  );
}

export default function MySocialStats({ currentUser }) {
  const [counts, setCounts] = useState(null);
  const [revision, setRevision] = useState(0);
  const [opened, setOpened] = useState(null);

  useEffect(() => {
    let active = true;
    let requestNumber = 0;

    async function load() {
      const request = ++requestNumber;

      const [followResult, postResult] = await Promise.allSettled([
        followApi.getStats(currentUser.id),
        feedApi.getUserPreview(currentUser.id),
      ]);

      if (!active || request !== requestNumber) return;

      setCounts({
        posts:
          postResult.status === "fulfilled" ? postResult.value.postCount : null,
        followers:
          followResult.status === "fulfilled"
            ? followResult.value.followerCount
            : null,
        following:
          followResult.status === "fulfilled"
            ? followResult.value.followingCount
            : null,
      });
    }

    function onVisible() {
      if (document.visibilityState === "visible") void load();
    }

    void load();
    window.addEventListener("focus", onVisible);
    document.addEventListener("visibilitychange", onVisible);

    return () => {
      active = false;
      window.removeEventListener("focus", onVisible);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [currentUser.id, revision]);

  const buttons = [
    { type: "posts", label: "게시글" },
    { type: "followers", label: "팔로워" },
    { type: "following", label: "팔로잉" },
  ];

  return (
    <>
      <div className="my-social-stats" aria-label="내 활동">
        {buttons.map(({ type, label }) => (
          <button
            type="button"
            key={type}
            aria-haspopup="dialog"
            onClick={() => {
              setOpened(type);
              setRevision((value) => value + 1);
            }}
          >
            <span>{label}</span>
            <strong>{counts === null ? "…" : (counts[type] ?? "—")}</strong>
          </button>
        ))}
      </div>

      {opened && (
        <SocialPanel
          key={`${currentUser.id}:${opened}`}
          type={opened}
          currentUser={currentUser}
          onChanged={() => setRevision((value) => value + 1)}
          onClose={() => {
            setOpened(null);
            setRevision((value) => value + 1);
          }}
        />
      )}
    </>
  );
}
