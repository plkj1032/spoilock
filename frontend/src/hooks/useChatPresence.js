import { useCallback, useRef, useState } from "react";
import { chatApi } from "../api/chatApi";

export default function useChatPresence(roomId) {
  const [members, setMembers] = useState([]);
  const [membersLoading, setMembersLoading] = useState(true);
  const [membersError, setMembersError] = useState("");

  const sessionRef = useRef(null);

  const stopPresence = useCallback(() => {
    const session = sessionRef.current;
    sessionRef.current = null;
    session?.stop();
  }, []);

  const refreshMembers = useCallback(() => {
    sessionRef.current?.refresh();
  }, []);

  const startPresence = useCallback(
    (client) => {
      stopPresence();

      let active = true;
      let fetching = false;
      let refreshAgain = false;

      const controller = new AbortController();

      setMembers([]);
      setMembersLoading(true);
      setMembersError("");

      async function refresh() {
        if (!active || !client.connected) return;

        // 조회 중 변경 알림이 오면 끝난 뒤 한 번 더 조회
        if (fetching) {
          refreshAgain = true;
          return;
        }

        fetching = true;

        try {
          const data = await chatApi.getMembers(Number(roomId), {
            signal: controller.signal,
          });

          if (!active) return;

          setMembers(data);
          setMembersError("");
        } catch (error) {
          if (!active || controller.signal.aborted) return;

          // 확인할 수 없는 접속 상태를 계속 표시하지 않음
          setMembers([]);
          setMembersError(
            error.status === 403
              ? "참여자 목록을 조회할 권한이 없습니다."
              : "참여자 목록을 불러오지 못했습니다.",
          );
        } finally {
          fetching = false;

          if (active) {
            setMembersLoading(false);

            if (refreshAgain) {
              refreshAgain = false;
              void refresh();
            }
          }
        }
      }

      function sendPresence() {
        if (!active || !client.connected) return;

        try {
          client.publish({
            destination: "/app/chat.presence",
            headers: {
              "content-type": "application/json",
            },
            body: JSON.stringify({
              roomId: Number(roomId),
            }),
          });
        } catch {
          if (active) {
            setMembersError("접속 상태를 갱신하지 못했습니다.");
          }
        }
      }

      function handleVisible() {
        if (document.visibilityState === "visible") {
          sendPresence();
          void refresh();
        }
      }

      // 접속 기록 유효 시간 90초보다 짧게 갱신
      const heartbeatTimer = window.setInterval(sendPresence, 20000);

      // Redis 알림 누락이나 만료된 접속 기록도 화면에 반영
      const refreshTimer = window.setInterval(() => {
        void refresh();
      }, 15000);

      document.addEventListener("visibilitychange", handleVisible);

      sessionRef.current = {
        refresh,
        stop() {
          active = false;
          controller.abort();

          window.clearInterval(heartbeatTimer);
          window.clearInterval(refreshTimer);

          document.removeEventListener("visibilitychange", handleVisible);
        },
      };

      sendPresence();
      void refresh();
    },
    [roomId, stopPresence],
  );

  return {
    members,
    membersLoading,
    membersError,
    startPresence,
    stopPresence,
    refreshMembers,
  };
}
