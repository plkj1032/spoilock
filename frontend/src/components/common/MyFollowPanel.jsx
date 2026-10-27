import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { followApi } from "../../api/followApi";
import FollowButton from "./FollowButton";
import Pagination from "./Pagination";

import "../../styles/MyFollowPanel.css";

function FollowList({
  currentUser,
  tab,
  page,
  onPageChange,
  onChanged,
  onRetry,
}) {
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;

    async function load() {
      try {
        const data =
          tab === "followers"
            ? await followApi.getFollowers(currentUser.id, page, 5)
            : await followApi.getFollowing(currentUser.id, page, 5);

        if (!active) return;
        setResult(data);
      } catch (err) {
        if (active) setError(err.message);
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();

    return () => {
      active = false;
    };
  }, [currentUser.id, tab, page]);

  if (loading) {
    return (
      <p className="my-follow-status" role="status">
        목록을 불러오는 중입니다.
      </p>
    );
  }

  if (error) {
    return (
      <div className="my-follow-error" role="alert">
        <p>{error}</p>
        <button type="button" onClick={onRetry}>
          다시 불러오기
        </button>
      </div>
    );
  }

  const users = result?.content ?? [];

  return (
    <>
      {users.length === 0 ? (
        <p className="my-follow-status">
          {tab === "followers"
            ? "아직 나를 팔로우하는 사용자가 없습니다."
            : "아직 팔로우한 사용자가 없습니다."}
        </p>
      ) : (
        <ul className="my-follow-list">
          {users.map((person) => (
            <li key={person.userId} className="my-follow-item">
              <div className="my-follow-avatar" aria-hidden="true">
                {Array.from(person.nickname?.trim() || "?")[0]}
              </div>

              <div className="my-follow-person">
                <strong>{person.nickname}</strong>
                <Link to={`/community/users/${person.userId}`}>
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

      <Pagination
        page={page}
        totalPages={result?.totalPages ?? 0}
        onPageChange={onPageChange}
        label={
          tab === "followers" ? "팔로워 목록 페이지" : "팔로잉 목록 페이지"
        }
      />
    </>
  );
}

export default function MyFollowPanel({ currentUser }) {
  const [stats, setStats] = useState(null);
  const [statsError, setStatsError] = useState("");
  const [revision, setRevision] = useState(0);

  const [tab, setTab] = useState(null);
  const [page, setPage] = useState(0);

  useEffect(() => {
    let active = true;
    let requestNumber = 0;

    async function loadStats() {
      const request = ++requestNumber;

      try {
        const data = await followApi.getStats(currentUser.id);

        if (!active || request !== requestNumber) return;

        setStats(data);
        setStatsError("");
      } catch (err) {
        if (!active || request !== requestNumber) return;
        setStatsError(err.message);
      }
    }

    function refreshWhenVisible() {
      if (document.visibilityState === "visible") {
        void loadStats();
      }
    }

    void loadStats();
    window.addEventListener("focus", refreshWhenVisible);
    document.addEventListener("visibilitychange", refreshWhenVisible);

    return () => {
      active = false;
      window.removeEventListener("focus", refreshWhenVisible);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
    };
  }, [currentUser.id, revision]);

  function selectTab(nextTab) {
    setTab(nextTab);
    setPage(0);
    setRevision((current) => current + 1);
  }

  function refreshAfterChange() {
    // 상대방의 통계가 아닌 내 통계를 다시 조회
    setPage(0);
    setRevision((current) => current + 1);
  }

  return (
    <section className="mypage-panel" aria-labelledby="my-follow-title">
      <div className="mypage-section-heading">
        <h2 id="my-follow-title">팔로우 관리</h2>

        {tab && (
          <button
            type="button"
            className="my-follow-collapse"
            onClick={() => setTab(null)}
          >
            목록 접기
          </button>
        )}
      </div>

      <div className="my-follow-counts">
        <button
          type="button"
          aria-pressed={tab === "followers"}
          onClick={() => selectTab("followers")}
        >
          <span>팔로워</span>
          <strong>{statsError ? "—" : (stats?.followerCount ?? "…")}</strong>
          <small>나를 팔로우하는 사람</small>
        </button>

        <button
          type="button"
          aria-pressed={tab === "following"}
          onClick={() => selectTab("following")}
        >
          <span>팔로잉</span>
          <strong>{statsError ? "—" : (stats?.followingCount ?? "…")}</strong>
          <small>내가 팔로우하는 사람</small>
        </button>
      </div>

      {statsError && (
        <div className="my-follow-error" role="alert">
          <p>팔로우 수를 불러오지 못했습니다. {statsError}</p>
          <button type="button" onClick={refreshAfterChange}>
            다시 불러오기
          </button>
        </div>
      )}

      {tab && (
        <FollowList
          key={`${currentUser.id}:${tab}:${page}:${revision}`}
          currentUser={currentUser}
          tab={tab}
          page={page}
          onPageChange={setPage}
          onChanged={refreshAfterChange}
          onRetry={refreshAfterChange}
        />
      )}
    </section>
  );
}
