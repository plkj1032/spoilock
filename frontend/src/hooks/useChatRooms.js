import { useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";

import { authApi } from "../api/authApi";
import { chatApi } from "../api/chatApi";

export default function useChatRooms(userId, revision = 0) {
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    let busy = false;
    let queued = false;
    let loaded = false;
    let client = null;

    const controller = new AbortController();

    async function refreshRooms() {
      if (!active) return;

      // 조회 도중 변경 알림을 받으면 완료 후 한 번 더 조회한다.
      if (busy) {
        queued = true;
        return;
      }

      busy = true;

      try {
        const data = await chatApi.getRooms({
          signal: controller.signal,
        });

        if (!active) return;

        loaded = true;
        setRooms(data ?? []);
        setError("");
      } catch (err) {
        if (!active || err.name === "AbortError") return;

        // 자동 갱신 실패 시 기존 목록은 유지한다.
        if (!loaded) {
          setError("채팅방 목록을 불러오지 못했습니다.");
        }
      } finally {
        busy = false;

        if (active) {
          setLoading(false);

          if (queued) {
            queued = false;
            void refreshRooms();
          }
        }
      }
    }

    function refreshWhenVisible() {
      if (document.visibilityState === "visible") {
        void refreshRooms();
      }
    }

    // 첫 목록 조회
    void refreshRooms();

    // 알림 유실이나 연결 장애를 보완한다.
    const timer = window.setInterval(refreshWhenVisible, 15000);

    document.addEventListener("visibilitychange", refreshWhenVisible);
    window.addEventListener("focus", refreshWhenVisible);
    window.addEventListener("online", refreshWhenVisible);

    if (userId != null) {
      const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";

      const useSecondServer =
        new URLSearchParams(window.location.search).get("server") === "8081";

      const secondaryUrl = import.meta.env.VITE_WS_SECONDARY_URL;

      client = new Client({
        brokerURL:
          useSecondServer && secondaryUrl
            ? secondaryUrl
            : `${protocol}//${window.location.host}/ws`,

        reconnectDelay: 5000,
        connectionTimeout: 5000,
        heartbeatIncoming: 0,
        heartbeatOutgoing: 10000,

        beforeConnect: async () => {
          if (!active) {
            void client.deactivate();
            return;
          }

          try {
            const me = await authApi.restore();

            if (!active || !me || String(me.id) !== String(userId)) {
              void client.deactivate();
            }
          } catch {
            // 목록 조회는 유지하고 인증을 확인할 수 없는 소켓은 종료한다.
            void client.deactivate();
          }
        },

        onConnect: () => {
          if (!active) {
            void client.deactivate();
            return;
          }

          // 서버의 구독 완료 후에도 조회해 연결 사이의 변경을 반영한다.
          const receiptId = `rooms-${crypto.randomUUID()}`;

          client.watchForReceipt(receiptId, () => {
            void refreshRooms();
          });

          client.subscribe(
            "/topic/chat.rooms",
            () => {
              void refreshRooms();
            },
            { receipt: receiptId },
          );

          void refreshRooms();
        },

        onStompError: () => {
          // 연결이 끊어지면 자동 재연결하며 목록은 그대로 유지한다.
        },

        onWebSocketError: () => {
          // 목록 자동 조회가 연결 장애를 보완한다.
        },
      });

      client.activate();
    }

    return () => {
      active = false;
      controller.abort();

      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
      window.removeEventListener("focus", refreshWhenVisible);
      window.removeEventListener("online", refreshWhenVisible);

      if (client) {
        void client.deactivate();
      }
    };
  }, [userId, revision]);

  return { rooms, loading, error };
}
