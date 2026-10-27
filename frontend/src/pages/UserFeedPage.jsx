import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";

import { feedApi } from "../api/feedApi";
import { FeedList } from "./CommunityPage";

import "../styles/FeedPage.css";

export default function UserFeedPage({ user, onLogin }) {
  const { userId } = useParams();

  const [preview, setPreview] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!user) return;

    let active = true;

    feedApi
      .getUserPreview(userId)
      .then((data) => {
        if (active) {
          setPreview(data);
          setError("");
        }
      })
      .catch((err) => {
        if (active) {
          setError(err.message);
        }
      });

    return () => {
      active = false;
    };
  }, [user, userId]);

  if (!user) {
    return (
      <main className="container main-content">
        <p>로그인 후 사용자 피드를 볼 수 있습니다.</p>

        <button type="button" onClick={onLogin}>
          로그인
        </button>
      </main>
    );
  }

  return (
    <main className="container main-content feed-page">
      <header className="feed-page__heading">
        <Link to="/community" className="feed-page__back">
          ← 전체 피드
        </Link>

        <h1>사용자 피드</h1>

        {preview && <p>게시글 {preview.postCount}개</p>}

        {error && <p role="alert">{error}</p>}
      </header>

      <FeedList
        key={`${user.id}:${userId}`}
        type="ALL"
        user={user}
        authorId={userId}
      />
    </main>
  );
}
