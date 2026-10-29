import { useCallback, useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";

import { apiRequest, authApi } from "../api/authApi";

export default function useRoomPage({
  userId,
  scope = "public",
  page = 0,
  size = 10,
  keyword = "",
  dramaId = "",
  roomType = "ALL",
  sort = "members",
  includeJoinedPrivate = false,
}) {
  const [revision, setRevision] = useState(0);
  const [result, setResult] = useState(null);

  const enabled = scope !== "mine" || userId != null;

  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  if (scope === "public") {
    params.set("sort", sort);

    if (keyword.trim()) {
      params.set("keyword", keyword.trim());
    }

    if (includeJoinedPrivate) {
      params.set("includeJoinedPrivate", "true");
    }
  }
  // 드라마·방 종류 필터는 마이페이지에서도 사용
  if (dramaId !== "" && dramaId != null) {
    params.set("dramaId", String(dramaId));
  }

  if (roomType && roomType !== "ALL") {
    params.set("roomType", roomType);
  }
  const path =
    scope === "mine"
      ? `/api/me/chat/rooms?${params}`
      : `/api/chat/rooms?${params}`;

  // 다른 계정이나 다른 검색 조건의 결과를 표시하지 않도록 구분
  const requestKey = `${userId ?? "guest"}:${path}`;

  const refresh = useCallback(() => {
    setRevision((current) => current + 1);
  }, []);

  // 페이지와 검색 조건이 변경되면 HTTP 조회만 다시 실행
  useEffect(() => {
    if (!enabled) return;

    let active = true;
    const controller = new AbortController();

    async function load() {
      try {
        const data = await apiRequest(path, {
          retryOn401: true,
          cache: "no-store",
          signal: controller.signal,
        });

        if (!active) return;

        setResult({
          key: requestKey,
          data,
          error: "",
        });
      } catch (error) {
        if (!active || error.name === "AbortError") return;

        // 같은 페이지를 자동 갱신하다 실패하면 기존 목록 유지
        setResult((previous) => ({
          key: requestKey,
          data: previous?.key === requestKey ? previous.data : null,
          error: error.message,
        }));
      }
    }

    void load();

    return () => {
      active = false;
      controller.abort();
    };
  }, [enabled, path, requestKey, revision]);

  // 연결 장애 중 놓친 변경과 다른 탭에서의 변경을 보완
  useEffect(() => {
    if (!enabled) return;

    function refreshWhenVisible() {
      if (document.visibilityState === "visible") {
        refresh();
      }
    }

    const timer = window.setInterval(refreshWhenVisible, 15000);

    window.addEventListener("focus", refreshWhenVisible);
    window.addEventListener("online", refreshWhenVisible);
    document.addEventListener("visibilitychange", refreshWhenVisible);

    return () => {
      window.clearInterval(timer);
      window.removeEventListener("focus", refreshWhenVisible);
      window.removeEventListener("online", refreshWhenVisible);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
    };
  }, [enabled, refresh]);

  // 페이지 변경과 관계없이 로그인 사용자별로 연결 유지
  useEffect(() => {
    if (!enabled || userId == null) return;

    let active = true;
    let refreshTimer = null;

    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";

    const useSecondServer =
      new URLSearchParams(window.location.search).get("server") === "8081";

    const secondaryUrl = import.meta.env.VITE_WS_SECONDARY_URL;

    function scheduleRefresh() {
      window.clearTimeout(refreshTimer);

      refreshTimer = window.setTimeout(() => {
        if (active) refresh();
      }, 150);
    }

    const client = new Client({
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
          // 인증 확인이 실패하면 HTTP 자동 조회만 유지
          void client.deactivate();
        }
      },

      onConnect: () => {
        if (!active) {
          void client.deactivate();
          return;
        }

        client.subscribe("/topic/chat.rooms", scheduleRefresh);

        // 연결이 끊겨 있던 동안의 변경 반영
        scheduleRefresh();
      },
    });

    client.activate();

    return () => {
      active = false;
      window.clearTimeout(refreshTimer);
      void client.deactivate();
    };
  }, [enabled, userId, refresh]);

  const matching = result?.key === requestKey;
  const data = matching ? result.data : null;

  return {
    rooms: data?.content ?? [],
    page: data?.page ?? page,
    totalPages: data?.totalPages ?? 0,
    totalElements: data?.totalElements ?? 0,
    loading: enabled && !matching,
    error: matching ? result.error : "",
    refresh,
  };
}
