import { useEffect, useState } from "react";

import { feedApi } from "../../api/feedApi";
import FeedCard from "./FeedCard";

import "../../styles/FeedPage.css";

export default function HomeFollowingFeed({
  user,
  onLogin,
  dramaId = null,
  summaryKey,
  onSummaryChange,
}) {
  const userId = user?.id;

  const [result, setResult] = useState(null);
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    if (userId == null) return;

    let active = true;
    let requesting = false;

    const controller = new AbortController();

    async function load() {
      if (!active || requesting) return;

      requesting = true;

      try {
        const data = await feedApi.getFeed({
          type: "FOLLOWING",
          size: 3,
          dramaId,
          signal: controller.signal,
        });

        if (!active) return;

        setResult({
          userId,
          items: data.items ?? [],
          error: "",
        });
        onSummaryChange?.({
          key: summaryKey,
          totalElements: data.totalElements,
          error: false,
        });
      } catch (error) {
        if (!active || error.name === "AbortError") return;
        onSummaryChange?.({
          key: summaryKey,
          totalElements: null,
          error: true,
        });

        setResult({
          userId,
          items: [],
          error: error.message || "팔로잉 피드를 불러오지 못했습니다.",
        });
      } finally {
        requesting = false;
      }
    }

    function refreshWhenVisible() {
      if (document.visibilityState === "visible") {
        void load();
      }
    }

    void load();

    const timer = window.setInterval(refreshWhenVisible, 15000);

    window.addEventListener("focus", refreshWhenVisible);
    document.addEventListener("visibilitychange", refreshWhenVisible);

    return () => {
      active = false;
      controller.abort();

      window.clearInterval(timer);
      window.removeEventListener("focus", refreshWhenVisible);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
    };
  }, [userId, dramaId, revision, summaryKey, onSummaryChange]);

  if (!user) {
    return (
      <div className="home-column-empty">
        <strong>팔로우한 사람들의 이야기를 만나보세요</strong>
        <p>로그인하면 팔로잉 사용자의 게시글을 볼 수 있어요.</p>

        <button
          type="button"
          className="button button-primary"
          onClick={onLogin}
        >
          로그인
        </button>
      </div>
    );
  }

  const current = result?.userId === userId ? result : null;

  if (!current) {
    return (
      <div className="home-column-empty" role="status">
        팔로잉 피드를 불러오는 중입니다.
      </div>
    );
  }

  if (current.error) {
    return (
      <div className="home-column-empty" role="alert">
        <p>{current.error}</p>

        <button
          type="button"
          className="button button-outline"
          onClick={() => setRevision((value) => value + 1)}
        >
          다시 불러오기
        </button>
      </div>
    );
  }

  if (current.items.length === 0) {
    return (
      <div className="home-column-empty">
        <strong>
          {dramaId
            ? "이 드라마의 팔로잉 게시글이 아직 없어요"
            : "아직 표시할 이야기가 없어요"}
        </strong>

        <p>
          내가 작성했거나 팔로우한 사람이 작성한
          <br />
          게시글이 여기에 표시돼요.
        </p>
      </div>
    );
  }

  return (
    <div
      className="home-following-scroll"
      role="region"
      aria-label="팔로잉 최신 게시글"
      tabIndex={0}
    >
      <div className="home-following-list">
        {current.items.map((post) => (
          <FeedCard key={post.postId} post={post} user={user} />
        ))}
      </div>
    </div>
  );
}
