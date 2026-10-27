import { useEffect, useState } from "react";
import { followApi } from "../../api/followApi";

export default function FollowButton({
  targetUserId,
  currentUser,
  onLogin,
  onChanged,
}) {
  const viewerId = currentUser?.id ?? "guest";

  return (
    <FollowButtonContent
      key={`${targetUserId}:${viewerId}`}
      targetUserId={targetUserId}
      currentUser={currentUser}
      onLogin={onLogin}
      onChanged={onChanged}
    />
  );
}

function FollowButtonContent({
  targetUserId,
  currentUser,
  onLogin,
  onChanged,
}) {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const viewerId = currentUser?.id;

  useEffect(() => {
    if (!viewerId) return;

    let active = true;

    followApi
      .getStats(targetUserId)
      .then((data) => {
        if (!active) return;

        setStats(data);
      })
      .catch((err) => {
        if (!active) return;

        setError(err.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [targetUserId, viewerId]);

  async function toggleFollow() {
    if (busy || !stats) return;

    setBusy(true);
    setError("");

    try {
      let updated;

      if (stats.isFollowing) {
        await followApi.unfollow(targetUserId);
        updated = await followApi.getStats(targetUserId);
      } else {
        updated = await followApi.follow(targetUserId);
      }

      setStats(updated);

      // 팝업의 팔로워 숫자도 즉시 갱신
      onChanged?.(updated);
    } catch (err) {
      if (err.status === 409) {
        // 다른 화면에서 먼저 팔로우했을 수 있으므로 동기화
        try {
          const updated = await followApi.getStats(targetUserId);
          setStats(updated);
          onChanged?.(updated);
        } catch {
          setError(err.message);
        }
      } else {
        setError(err.message);
      }
    } finally {
      setBusy(false);
    }
  }

  if (!currentUser) {
    return (
      <button
        type="button"
        className="button button-primary"
        onClick={() => onLogin?.()}
      >
        팔로우
      </button>
    );
  }

  // 내 계정에는 팔로우 버튼을 표시하지 않음
  if (String(viewerId) === String(targetUserId)) {
    return null;
  }

  return (
    <div className="follow-button-wrapper">
      <button
        type="button"
        className={
          stats?.isFollowing ? "button button-outline" : "button button-primary"
        }
        onClick={toggleFollow}
        disabled={loading || busy || !stats}
        aria-pressed={stats?.isFollowing ?? false}
      >
        {busy
          ? "처리 중..."
          : loading
            ? "확인 중..."
            : stats?.isFollowing
              ? "✓ 팔로잉"
              : "+ 팔로우"}
      </button>

      {error && (
        <p role="alert" className="follow-button-error">
          {error}
        </p>
      )}
    </div>
  );
}
