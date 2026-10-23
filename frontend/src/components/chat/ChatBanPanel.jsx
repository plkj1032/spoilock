import { useEffect, useRef, useState } from "react";
import { chatApi } from "../../api/chatApi";
import "../../styles/ChatBanPanel.css";
import Button from "../common/Button";

export default function ChatBanPanel({ roomId }) {
  const [bans, setBans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [releasingId, setReleasingId] = useState(null);
  const releasingRef = useRef(false);

  useEffect(() => {
    let active = true;

    async function loadBans() {
      try {
        const data = await chatApi.getBans(roomId);
        if (active) setBans(data);
      } catch (err) {
        if (active) {
          setError(
            err.status === 403
              ? "강퇴 목록을 조회할 권한이 없습니다."
              : err.message,
          );
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    loadBans();

    return () => {
      active = false;
    };
  }, [roomId]);

  async function releaseBan(ban) {
    if (releasingRef.current) return;

    releasingRef.current = true;
    setReleasingId(ban.banId);
    setError("");

    try {
      await chatApi.releaseBan(roomId, ban.banId);

      setBans((current) => current.filter((item) => item.banId !== ban.banId));
    } catch (err) {
      setError(
        err.status === 403 ? "강퇴를 해제할 권한이 없습니다." : err.message,
      );
    } finally {
      releasingRef.current = false;
      setReleasingId(null);
    }
  }

  return (
    <section aria-label="강퇴 관리" className="chat-ban-panel">
      <h2>강퇴 관리</h2>
      <p>해제된 회원은 채팅방에 다시 입장할 수 있습니다.</p>

      {loading && <p role="status">목록을 불러오는 중입니다.</p>}
      {error && <p role="alert">{error}</p>}

      {!loading && !error && bans.length === 0 && (
        <p>현재 강퇴된 회원이 없습니다.</p>
      )}

      {bans.map((ban) => (
        <div key={ban.banId} className="chat-ban-panel__item">
          <strong>{ban.nickname}</strong>
          <p>사유: {ban.reason || "입력된 사유 없음"}</p>
          <p>강퇴 시각: {new Date(ban.createdAt).toLocaleString("ko-KR")}</p>

          <Button
            variant="outline"
            disabled={releasingId !== null}
            onClick={() => releaseBan(ban)}
          >
            {releasingId === ban.banId ? "해제 중..." : "강퇴 해제"}
          </Button>
        </div>
      ))}
    </section>
  );
}
