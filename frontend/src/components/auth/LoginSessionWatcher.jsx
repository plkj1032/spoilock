import { useEffect } from "react";
import { Client } from "@stomp/stompjs";

import { authApi } from "../../api/authApi";

export default function LoginSessionWatcher({ userId, onSessionExpired }) {
  useEffect(() => {
    if (userId == null) return;

    let active = true;
    let checking = null;
    let expired = false;

    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";

    const useSecondServer =
      new URLSearchParams(window.location.search).get("server") === "8081";

    const secondaryUrl = import.meta.env.VITE_WS_SECONDARY_URL;

    const brokerURL =
      useSecondServer && secondaryUrl
        ? secondaryUrl
        : `${protocol}//${window.location.host}/ws`;

    function expireSession() {
      if (!active || expired) return;

      expired = true;
      void client.deactivate();
      onSessionExpired();
    }

    async function checkSession() {
      if (!active || expired) return false;

      if (checking) return checking;

      checking = (async () => {
        try {
          const me = await authApi.restore();

          if (!active || expired) return false;

          if (!me || String(me.id) !== String(userId)) {
            expireSession();
            return false;
          }

          return true;
        } catch {
          // 일시적인 네트워크 오류만으로 로그아웃하지 않음
          return false;
        } finally {
          checking = null;
        }
      })();

      return checking;
    }

    const client = new Client({
      brokerURL,
      reconnectDelay: 5000,
      connectionTimeout: 5000,
      heartbeatIncoming: 0,
      heartbeatOutgoing: 10000,

      beforeConnect: async () => {
        if (!active || expired) {
          void client.deactivate();
          return;
        }

        await checkSession();

        if (!active || expired) {
          void client.deactivate();
        }
      },

      onConnect: () => {
        // 연결이 끊긴 동안 발생한 로그인 변경도 확인
        void checkSession();
      },

      onWebSocketClose: () => {
        // 서버에서 이전 로그인 연결을 종료하면 즉시 인증 확인
        if (active && !expired) {
          void checkSession();
        }
      },

      onStompError: () => {
        if (active && !expired) {
          void checkSession();
        }
      },
    });

    function checkWhenVisible() {
      if (document.visibilityState === "visible") {
        void checkSession();
      }
    }

    function checkWhenOnline() {
      void checkSession();
    }

    // WebSocket 연결이 불가능한 환경에서도 화면 상태를 보완
    const timer = window.setInterval(checkWhenVisible, 30000);

    window.addEventListener("focus", checkWhenVisible);
    window.addEventListener("online", checkWhenOnline);
    document.addEventListener("visibilitychange", checkWhenVisible);

    client.activate();

    return () => {
      active = false;

      window.clearInterval(timer);
      window.removeEventListener("focus", checkWhenVisible);
      window.removeEventListener("online", checkWhenOnline);
      document.removeEventListener("visibilitychange", checkWhenVisible);

      void client.deactivate();
    };
  }, [userId, onSessionExpired]);

  return null;
}
