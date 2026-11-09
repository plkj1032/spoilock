import { useEffect, useRef, useState, useLayoutEffect } from "react";
import {
  Link,
  useNavigate,
  useParams,
  useSearchParams,
} from "react-router-dom";
import { Client } from "@stomp/stompjs";
import { chatApi } from "../api/chatApi";
import ChatBanPanel from "../components/chat/ChatBanPanel";
import { apiRequest, authApi } from "../api/authApi";
import Button from "../components/common/Button";
import "../styles/ChatRoomPage.css";
import "../styles/ChatScrollControls.css";
import useChatPresence from "../hooks/useChatPresence";
import "../styles/ChatPresence.css";
import ChatMemberPanel from "../components/chat/ChatMemberPanel";
import ChatReportModal from "../components/chat/ChatReportModal";
import UserFollowDialog from "../components/user/UserFollowDialog";
import ChatFollowingInviteModal from "../components/chat/ChatFollowingInviteModal";
import ChatOwnershipModal from "../components/chat/ChatOwnershipModal";

// 서버가 원문 대신 보내는 차단 이유별 안내 (사건 내용은 절대 표시하지 않음)
const BLOCK_MESSAGES = {
  ANALYZING: "⏳ 스포일러 여부를 확인하고 있어요…",
  NOT_WATCHED: "🔒 아직 시청하지 않은 회차의 내용이 포함된 메시지예요.",
  REVIEW: "🔒 스포일러 여부를 확인할 수 없어 가려진 메시지예요.",
  FAILED: "⏳ 확인이 지연되고 있어요. 잠시 후 다시 표시돼요.",
  HIDDEN: "🚫 관리자가 숨긴 메시지예요.",
  DEFAULT: "🔒 아직 공개되지 않은 메시지입니다.",
};

// Presentation only: keep the server visibility decision and never render a blocked body.
function ChatMessageGuard({ reason }) {
  const title = {
    NOT_WATCHED: "스포일러 보호 중",
    ANALYZING: "스포일러 확인 중",
    REVIEW: "메시지 검토 중",
    FAILED: "확인 지연 중",
    HIDDEN: "숨겨진 메시지",
  }[reason] ?? "보호 중인 메시지";
  const icon = reason === "ANALYZING" || reason === "FAILED"
    ? "⏳"
    : reason === "HIDDEN" ? "🚫" : "🔒";
  const description = (BLOCK_MESSAGES[reason] ?? BLOCK_MESSAGES.DEFAULT)
    .replace(/^(?:🔒|⏳|🚫)\s*/u, "");

  return (
    <div className="chat-message-guard">
      <span className="chat-message-guard__icon" aria-hidden="true">{icon}</span>
      <strong>{title}</strong>
      <p>{description}</p>
    </div>
  );
}

// 같은 메시지는 하나만 유지하고, ID 순으로 정렬
function mergeMessages(current, incoming) {
  const merged = new Map(
    current.map((message) => [message.messageId, message]),
  );

  for (const message of incoming) {
    merged.set(message.messageId, message);
  }

  return [...merged.values()].sort((a, b) => a.messageId - b.messageId);
}

export default function ChatRoomPage({ user, onNotice }) {
  const { roomId } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [showOwnershipModal, setShowOwnershipModal] = useState(false);
  const leavingRef = useRef(false);
  const previousOwnerRef = useRef(null);

  // 개발 환경에서만 두 번째 서버 연결 허용
  const useSecondServer =
    import.meta.env.DEV && searchParams.get("server") === "8081";

  const clientRef = useRef(null);
  const messageListRef = useRef(null);
  const followBottomRef = useRef(true);
  const initialScrollDoneRef = useRef(false);

  const [room, setRoom] = useState(null);
  const [messages, setMessages] = useState([]);
  const [content, setContent] = useState("");
  const [connected, setConnected] = useState(false);
  const [reconnecting, setReconnecting] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [leaving, setLeaving] = useState(false);
  const [nextCursor, setNextCursor] = useState(null);
  const [hasNext, setHasNext] = useState(false);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [kickingUserId, setKickingUserId] = useState(null);
  const [kickError, setKickError] = useState("");
  const [showMemberPanel, setShowMemberPanel] = useState(false);
  const [showBanPanel, setShowBanPanel] = useState(false);
  const [connectionAttempt, setConnectionAttempt] = useState(0);
  const kickingRef = useRef(false);
  const [bans, setBans] = useState([]);
  const [bansLoading, setBansLoading] = useState(true);
  const [bansError, setBansError] = useState("");
  const [banRevision, setBanRevision] = useState(0);
  const historyAnchorRef = useRef(null);
  const [isAwayFromBottom, setIsAwayFromBottom] = useState(false);
  const [newMessageCount, setNewMessageCount] = useState(0);
  const seenMessageIdsRef = useRef(new Set());
  const [reportTarget, setReportTarget] = useState(null);
  const [profileTarget, setProfileTarget] = useState(null);
  const [showInviteModal, setShowInviteModal] = useState(false);
  const {
    members,
    membersLoading,
    membersError,
    startPresence,
    stopPresence,
    refreshMembers,
  } = useChatPresence(roomId);

  const onlineUserIds = new Set(
    connected
      ? members
          .filter((member) => member.online)
          .map((member) => String(member.userId))
      : [],
  );
  function goToBottom() {
    const list = messageListRef.current;
    if (!list) return;

    historyAnchorRef.current = null;
    followBottomRef.current = true;
    list.scrollTop = list.scrollHeight;

    setIsAwayFromBottom(false);
    setNewMessageCount(0);
  }

  function handleMessageScroll(event) {
    const list = event.currentTarget;
    const distance = list.scrollHeight - list.scrollTop - list.clientHeight;

    const away = distance > 80;

    followBottomRef.current = !away;
    setIsAwayFromBottom(away);

    if (!away) {
      setNewMessageCount(0);
    }
  }
  const bannedUserIds = new Set(bans.map((ban) => String(ban.userId)));
  useEffect(() => {
    if (!room || room.createdById == null) return;

    const current = {
      roomId: String(roomId),
      ownerId: String(room.createdById),
    };

    const previous = previousOwnerRef.current;

    // 먼저 기록해서 같은 정보가 다시 수신되어도 중복 알림 방지
    previousOwnerRef.current = current;

    // 최초 조회에서는 알림을 띄우지 않음
    if (!previous || previous.roomId !== current.roomId) return;

    const becameOwner =
      ["CUSTOM", "PRIVATE"].includes(room.roomType) &&
      previous.ownerId !== current.ownerId &&
      current.ownerId === String(user.id);

    if (becameOwner) {
      onNotice("방장이 되었습니다.");
    }
  }, [room, roomId, user.id, onNotice]);
  const canKick = Boolean(
    room &&
    (room.roomType === "DEFAULT"
      ? user.role === "ADMIN"
      : ["CUSTOM", "PRIVATE"].includes(room.roomType) &&
        String(room.createdById) === String(user.id)),
  );
  const canInvite = Boolean(
    room?.roomType === "PRIVATE" &&
    user?.id != null &&
    String(room.createdById) === String(user.id),
  );
  useEffect(() => {
    if (!canKick) return;

    let active = true;

    chatApi
      .getBans(Number(roomId))
      .then((data) => {
        if (!active) return;

        setBans(data);
        setBansError("");
      })
      .catch((err) => {
        if (!active) return;

        setBansError(
          err.status === 403
            ? "강퇴 목록을 조회할 권한이 없습니다."
            : "강퇴 목록을 불러오지 못했습니다.",
        );
      })
      .finally(() => {
        if (active) setBansLoading(false);
      });

    return () => {
      active = false;
    };
  }, [roomId, canKick, banRevision]);

  useLayoutEffect(() => {
    const list = messageListRef.current;
    if (!list || loading) return;

    const anchor = historyAnchorRef.current;

    if (anchor) {
      const element = Array.from(
        list.querySelectorAll("[data-message-id]"),
      ).find((item) => item.dataset.messageId === anchor.messageId);

      if (element) {
        const currentOffset =
          element.getBoundingClientRect().top -
          list.getBoundingClientRect().top;

        list.scrollTop += currentOffset - anchor.offset;
      }

      historyAnchorRef.current = null;
      return;
    }

    if (!initialScrollDoneRef.current || followBottomRef.current) {
      list.scrollTop = list.scrollHeight;
      initialScrollDoneRef.current = true;
    }
  }, [messages, loading]);

  const olderLoadingRef = useRef(false);
  useEffect(() => {
    let active = true;

    chatApi
      .getRoom(Number(roomId))
      .then((data) => {
        if (active) {
          setRoom(data);
        }
      })
      .catch((error) => {
        if (active) {
          setError(error.message);
        }
      });

    return () => {
      active = false;
    };
  }, [roomId]);

  useEffect(() => {
    let active = true;
    const controller = new AbortController();

    let historyLoaded = false;
    let historyCheckpoint = 0;
    let historyRequestId = 0;
    let historyRetryTimer = null;
    let historyLiveIds = null;

    const HISTORY_RECOVERY_ERROR =
      "일부 대화를 불러오지 못해 자동으로 다시 확인하고 있어요.";

    async function getJson(path) {
      try {
        return await apiRequest(path, {
          retryOn401: true,
          cache: "no-store",
          signal: controller.signal,
        });
      } catch (err) {
        if (err.status === 403) {
          throw new Error("이 채팅방에 접근할 권한이 없습니다.", {
            cause: err,
          });
        }

        throw err;
      }
    }

    function showError(err) {
      if (active && !controller.signal.aborted) {
        setError(err.message);
      }
    }

    async function loadHistory() {
      const requestId = ++historyRequestId;
      const firstLoad = !historyLoaded;
      const checkpoint = historyCheckpoint;

      const liveIds = new Set();
      historyLiveIds = liveIds;

      window.clearTimeout(historyRetryTimer);
      historyRetryTimer = null;

      const collected = [];
      const visitedCursors = new Set();

      let cursor = null;
      let firstPage = null;

      try {
        while (active && requestId === historyRequestId) {
          const path =
            cursor === null
              ? `/api/chat/rooms/${roomId}/messages`
              : `/api/chat/rooms/${roomId}/messages?cursor=${encodeURIComponent(cursor)}`;

          const data = await getJson(path);

          if (!active || requestId !== historyRequestId) return;

          if (firstPage === null) {
            firstPage = data;
          }

          collected.push(...data.messages);

          const reachedCheckpoint = data.messages.some(
            (message) => Number(message.messageId) <= checkpoint,
          );

          // 첫 입장은 최근 30개만 표시.
          // 재연결은 이전 조회 지점까지 계속 내려가며 복구.
          if (firstLoad || reachedCheckpoint || !data.hasNext) {
            break;
          }

          const next = data.nextCursor;

          if (
            next == null ||
            data.messages.length === 0 ||
            visitedCursors.has(String(next)) ||
            (cursor !== null && Number(next) >= Number(cursor))
          ) {
            throw new Error("메시지 페이지 정보를 확인하지 못했습니다.");
          }

          visitedCursors.add(String(next));
          cursor = next;
        }

        if (!active || requestId !== historyRequestId || !firstPage) return;

        setMessages((current) => {
          // 이력 조회 중 실시간으로 갱신된 메시지는 최신 값을 유지
          const currentIds = new Set(
            current.map((message) => String(message.messageId)),
          );

          const historyMessages = collected.filter((message) => {
            const id = String(message.messageId);
            return !liveIds.has(id) || !currentIds.has(id);
          });

          return mergeMessages(current, historyMessages);
        });

        collected.forEach((message) => {
          seenMessageIdsRef.current.add(String(message.messageId));
        });

        if (firstLoad) {
          setNextCursor(firstPage.nextCursor);
          setHasNext(firstPage.hasNext);
        }

        // 전체 복구가 성공한 경우에만 기준점을 앞으로 이동
        historyCheckpoint = firstPage.messages.reduce(
          (latestId, message) => Math.max(latestId, Number(message.messageId)),
          checkpoint,
        );

        historyLoaded = true;

        setError((current) =>
          current === HISTORY_RECOVERY_ERROR ? "" : current,
        );
      } catch (err) {
        if (
          !active ||
          controller.signal.aborted ||
          requestId !== historyRequestId
        ) {
          return;
        }

        const status = err.status ?? err.cause?.status;

        if (status === 401 || status === 403 || status === 404) {
          stopWithError(
            status === 401
              ? "로그인이 만료되었습니다. 다시 로그인해 주세요."
              : "이 채팅방의 대화를 조회할 수 없습니다.",
          );
          return;
        }

        setError(HISTORY_RECOVERY_ERROR);

        // 조회 실패 시 기준점은 유지하고 다시 시도
        historyRetryTimer = window.setTimeout(() => {
          if (active && !stopped && client.connected) {
            void loadHistory();
          }
        }, 5000);
      } finally {
        if (active && requestId === historyRequestId) {
          setLoading(false);

          if (historyLiveIds === liveIds) {
            historyLiveIds = null;
          }
        }
      }
    }
    async function receiveMessage(frame) {
      try {
        const event = JSON.parse(frame.body);

        if (String(event.roomId) !== roomId) return;
        if (!active) return;
        if (event.eventType === "PRESENCE_CHANGED") {
          // 다음 단계에서 회원 목록을 다시 조회하도록 연결
          refreshMembers();
          return;
        }
        if (event.eventType == "KICKED") {
          setConnected(false);
          setMessages([]);
          setContent("");

          window.alert("이 채팅방에서 강퇴되었습니다.");
          navigate("/chat", { replace: true });
          return;
        }

        const message = await getJson(
          `/api/chat/rooms/${roomId}/messages/${event.messageId}`,
        );
        if (active && message.messageType !== "CHAT") {
          refreshMembers();

          // 방장 위임 후 퇴장 메시지를 받으면 새 방장 정보도 반영
          void getJson(`/api/chat/rooms/${roomId}`)
            .then((latestRoom) => {
              if (active) setRoom(latestRoom);
            })
            .catch(showError);
        }

        if (active) {
          const messageId = String(message.messageId);

          // 조회 중 도착한 최신 메시지 정보를 이력 데이터로 덮어쓰지 않음
          historyLiveIds?.add(messageId);

          const isNew = !seenMessageIdsRef.current.has(messageId);

          seenMessageIdsRef.current.add(messageId);

          if (
            isNew &&
            initialScrollDoneRef.current &&
            !followBottomRef.current &&
            message.messageType === "CHAT" &&
            String(message.userId) !== String(user.id)
          ) {
            setNewMessageCount((current) => current + 1);
          }

          setMessages((current) => mergeMessages(current, [message]));
        }
      } catch (err) {
        showError(err);
      }
    }

    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";

    let stopped = false;
    let hasConnected = false;
    let recoveryNoticeTimer = null;
    let authRetryTimer = null;

    function clearRecoveryNotice() {
      window.clearTimeout(recoveryNoticeTimer);
      recoveryNoticeTimer = null;
    }

    function beginRecovery() {
      if (!active || stopped) return;

      // 이전 연결에서 시작한 이력 조회 결과는 반영하지 않음
      historyRequestId += 1;
      historyLiveIds = null;

      window.clearTimeout(historyRetryTimer);
      historyRetryTimer = null;

      stopPresence();
      setConnected(false);
      setReconnecting(true);

      // 짧은 자동 복구 중에는 오류를 표시하지 않음
      if (recoveryNoticeTimer === null) {
        recoveryNoticeTimer = window.setTimeout(() => {
          recoveryNoticeTimer = null;

          if (!active || stopped || client.connected) return;

          setLoading(false);
          setError(
            "채팅 연결을 복구하고 있어요. 네트워크 상태를 확인해 주세요.",
          );
        }, 15000);
      }
    }

    function stopWithError(message) {
      historyRequestId += 1;
      historyLiveIds = null;
      window.clearTimeout(historyRetryTimer);
      stopped = true;

      clearRecoveryNotice();
      window.clearTimeout(authRetryTimer);

      stopPresence();
      setConnected(false);
      setReconnecting(false);
      setLoading(false);
      setError(message);

      void client.deactivate();
    }

    const client = new Client({
      brokerURL: useSecondServer
        ? import.meta.env.VITE_WS_SECONDARY_URL
        : `${protocol}//${window.location.host}/ws`,

      reconnectDelay: 2000,
      connectionTimeout: 5000,

      beforeConnect: async () => {
        if (!active || stopped) {
          void client.deactivate();
          return;
        }

        if (!hasConnected) {
          setLoading(true);
        }

        try {
          // 액세스 토큰이 만료됐다면 기존 refresh 흐름으로 갱신
          const me = await authApi.restore();

          if (!active || stopped) {
            void client.deactivate();
            return;
          }

          if (!me || String(me.id) !== String(user.id)) {
            stopWithError(
              "로그인 상태가 변경되었습니다. 다시 로그인해 주세요.",
            );
            return;
          }

          // 재연결 전에 강퇴·방 삭제 등 접근 권한도 확인
          const latestRoom = await getJson(`/api/chat/rooms/${roomId}`);

          if (!active || stopped) {
            void client.deactivate();
            return;
          }

          setRoom(latestRoom);
        } catch (err) {
          if (!active || stopped || controller.signal.aborted) return;

          const status = err.status ?? err.cause?.status;

          if (status === 401) {
            stopWithError("로그인이 만료되었습니다. 다시 로그인해 주세요.");
            return;
          }

          if (status === 403 || status === 404) {
            stopWithError(
              "이 채팅방에 접근할 수 없습니다. 참여 상태를 확인해 주세요.",
            );
            return;
          }

          // 인증 서버의 일시적 장애도 자동 재시도
          beginRecovery();

          void client.deactivate().then(() => {
            if (!active || stopped) return;

            window.clearTimeout(authRetryTimer);
            authRetryTimer = window.setTimeout(() => {
              if (active && !stopped) {
                client.activate();
              }
            }, 3000);
          });
        }
      },

      onConnect: () => {
        if (!active || stopped) {
          void client.deactivate();
          return;
        }

        hasConnected = true;
        clearRecoveryNotice();

        client.subscribe("/user/queue/chat", receiveMessage);

        setConnected(true);
        setReconnecting(false);
        setError("");

        startPresence(client);

        // 연결이 끊긴 동안의 최근 대화도 다시 조회
        void loadHistory();
      },

      onStompError: () => {
        if (!active || stopped) return;

        beginRecovery();

        // deactivate와 달리 자동 재연결을 유지
        client.forceDisconnect();
      },

      onWebSocketError: () => {
        if (!active || stopped) return;
        beginRecovery();
      },

      onWebSocketClose: () => {
        if (!active || stopped) return;
        beginRecovery();
      },
    });

    clientRef.current = client;
    client.activate();

    return () => {
      active = false;
      stopped = true;

      historyRequestId += 1;
      historyLiveIds = null;
      window.clearTimeout(historyRetryTimer);

      clearRecoveryNotice();
      window.clearTimeout(authRetryTimer);

      stopPresence();
      controller.abort();

      if (clientRef.current === client) {
        clientRef.current = null;
      }

      void client.deactivate();
    };
  }, [
    roomId,
    user.id,
    useSecondServer,
    navigate,
    connectionAttempt,
    startPresence,
    stopPresence,
    refreshMembers,
  ]);
  async function loadOlderMessages() {
    if (!hasNext || nextCursor === null || olderLoadingRef.current) {
      return;
    }

    // 조회 중 다른 페이지로 이동했는지 확인할 기준
    const currentClient = clientRef.current;

    olderLoadingRef.current = true;
    setLoadingOlder(true);
    setError("");

    try {
      const data = await apiRequest(
        `/api/chat/rooms/${roomId}/messages?cursor=${nextCursor}`,
        {
          retryOn401: true,
          cache: "no-store",
        },
      );

      if (clientRef.current !== currentClient) return;
      data.messages.forEach((message) => {
        seenMessageIdsRef.current.add(String(message.messageId));
      });

      const list = messageListRef.current;

      if (list) {
        const listTop = list.getBoundingClientRect().top;

        // 현재 화면에 보이는 첫 번째 메시지를 기준점으로 기록
        const firstVisible = Array.from(
          list.querySelectorAll("[data-message-id]"),
        ).find((element) => element.getBoundingClientRect().bottom > listTop);

        if (firstVisible) {
          historyAnchorRef.current = {
            messageId: firstVisible.dataset.messageId,
            offset: firstVisible.getBoundingClientRect().top - listTop,
          };
        }
      }

      followBottomRef.current = false;

      setMessages((current) => mergeMessages(data.messages, current));

      setNextCursor(data.nextCursor);
      setHasNext(data.hasNext);
    } catch (err) {
      if (clientRef.current === currentClient) {
        setError(
          err.status === 403
            ? "이 채팅방에 접근할 권한이 없습니다."
            : err.message,
        );
      }
    } finally {
      olderLoadingRef.current = false;

      if (clientRef.current === currentClient) {
        setLoadingOlder(false);
      }
    }
  }
  async function kickMember(targetUserId, nickname) {
    if (
      !canKick ||
      !connected ||
      kickingRef.current ||
      bansLoading ||
      bansError ||
      String(targetUserId) === String(user.id) ||
      bannedUserIds.has(String(targetUserId))
    ) {
      return;
    }

    setKickError("");

    const reason = window.prompt(
      `${nickname}님을 강퇴합니다.\n사유를 입력하세요. 비워두어도 됩니다.`,
      "",
    );

    if (reason === null) return;

    if (reason.length > 500) {
      const message = "강퇴 사유는 500자 이하로 입력해 주세요.";
      setKickError(message);
      setError(message);
      return;
    }

    kickingRef.current = true;
    setKickingUserId(targetUserId);
    setError("");

    try {
      await chatApi.kickMember(
        Number(roomId),
        targetUserId,
        reason.trim() || null,
      );

      // 두 목록을 모두 갱신
      setBansLoading(true);
      setBanRevision((current) => current + 1);
      refreshMembers();
    } catch (err) {
      const messages = {
        401: "로그인 상태를 확인해 주세요.",
        403: "강퇴 권한이 없거나 인증 상태가 변경되었습니다.",
        404: "현재 방에 참여 중인 회원이 아닙니다.",
        409: "이미 강퇴된 회원입니다.",
      };

      const message = messages[err.status] || err.message;

      // 참여자 모달과 기존 채팅 화면 양쪽에서 오류 확인 가능
      setKickError(message);
      setError(message);

      if (err.status === 409) {
        setBansLoading(true);
        setBanRevision((current) => current + 1);
      }

      if (err.status === 404 || err.status === 409) {
        refreshMembers();
      }
    } finally {
      kickingRef.current = false;
      setKickingUserId(null);
    }
  }
  async function leaveRoom() {
    if (leavingRef.current) return;

    leavingRef.current = true;
    setLeaving(true);
    setError("");

    try {
      // 화면에 남아 있는 예전 방장 정보 대신 최신 정보 확인
      const latestRoom = await chatApi.getRoom(Number(roomId));
      setRoom(latestRoom);

      const isOwner =
        ["CUSTOM", "PRIVATE"].includes(latestRoom.roomType) &&
        String(latestRoom.createdById) === String(user.id);

      if (isOwner) {
        const latestMembers = await chatApi.getMembers(Number(roomId));
        const hasOthers = latestMembers.some(
          (member) => String(member.userId) !== String(user.id),
        );

        if (hasOthers) {
          setShowMemberPanel(false);
          setShowBanPanel(false);
          setShowInviteModal(false);
          setProfileTarget(null);
          setShowOwnershipModal(true);
          return;
        }
      }

      await chatApi.leaveRoom(Number(roomId));
      navigate("/", { replace: true });
    } catch (error) {
      if (error.status === 409) {
        // 확인 직후 다른 사람이 가입했을 수도 있으므로 패널에서 재확인
        setShowOwnershipModal(true);
      } else {
        setError(error.message);
      }
    } finally {
      leavingRef.current = false;
      setLeaving(false);
    }
  }
  function sendMessage(event) {
    event.preventDefault();

    const text = content.trim();
    const client = clientRef.current;

    if (!text) return;

    if (!connected || !client?.connected) {
      if (!reconnecting) {
        setError("채팅 서버 연결을 확인해 주세요.");
      }

      // 입력한 내용은 그대로 유지
      return;
    }

    try {
      followBottomRef.current = true;

      client.publish({
        destination: "/app/chat.send",
        headers: {
          "content-type": "application/json",
        },
        body: JSON.stringify({
          roomId: Number(roomId),
          content: text,
        }),
      });

      setContent("");
      setError("");
    } catch {
      setError("메시지를 전송하지 못했습니다.");
    }
  }

  function openMemberProfile(member) {
    // 다른 모달이 떠 있지 않도록 정리
    setShowMemberPanel(false);
    setShowBanPanel(false);
    setReportTarget(null);

    // 선택한 사용자의 프로필 열기
    setProfileTarget({
      userId: member.userId,
      nickname: member.nickname,
    });
  }

  return (
    <main className="chat-room-page">
      <section className="chat-room-shell" aria-label="채팅방">
        <header className="chat-room-header">
          <Link to="/chat">← 채팅방 목록</Link>

          <div className="chat-room-heading">
            <div>
              <h1>{room?.roomName ?? "채팅방"}</h1>
              <p>
                {user.nickname}님
                {connected ? " · 연결됨" : reconnecting ? "" : " · 연결 안 됨"}
              </p>
            </div>

            <div className="chat-room-actions">
              {canInvite && (
                <Button
                  variant="outline"
                  aria-haspopup="dialog"
                  disabled={leaving}
                  onClick={() => setShowInviteModal(true)}
                >
                  ✉️ 초대
                </Button>
              )}
              {canKick && (
                <Button
                  variant="outline"
                  aria-expanded={showBanPanel}
                  onClick={() => {
                    setShowMemberPanel(false);

                    if (!showBanPanel) {
                      setBansLoading(true);
                      setBanRevision((current) => current + 1);
                    }

                    setShowBanPanel((current) => !current);
                  }}
                >
                  {showBanPanel ? "강퇴 관리 닫기" : "강퇴 관리"}
                </Button>
              )}
              <Button
                variant="outline"
                aria-haspopup="dialog"
                aria-expanded={showMemberPanel}
                disabled={!connected}
                onClick={() => {
                  setShowBanPanel(false);
                  setShowMemberPanel(true);
                  refreshMembers();
                }}
              >
                참여자
                {!membersLoading && !membersError && ` ${members.length}`}
              </Button>

              <Button variant="outline" onClick={leaveRoom} disabled={leaving}>
                {leaving ? "나가는 중..." : "채팅방 나가기"}
              </Button>
            </div>
          </div>

          {loading && <p role="status">대화를 불러오는 중입니다.</p>}
          {error && <p role="alert">{error}</p>}

          {!connected && !loading && !reconnecting && (
            <Button
              variant="outline"
              onClick={() => {
                setLoading(true);
                setConnectionAttempt((current) => current + 1);
              }}
            >
              다시 연결
            </Button>
          )}
        </header>
        {showOwnershipModal && (
          <ChatOwnershipModal
            key={roomId}
            roomId={Number(roomId)}
            currentUserId={user.id}
            roomName={room?.roomName ?? "채팅방"}
            onClose={() => setShowOwnershipModal(false)}
            onLeft={() => {
              setShowOwnershipModal(false);
              navigate("/", { replace: true });
            }}
          />
        )}
        {canInvite && showInviteModal && (
          <ChatFollowingInviteModal
            key={roomId}
            roomId={Number(roomId)}
            roomName={room.roomName}
            onClose={() => setShowInviteModal(false)}
          />
        )}

        {showMemberPanel && (
          <ChatMemberPanel
            key={roomId}
            roomType={room?.roomType}
            ownerUserId={room?.createdById}
            members={members}
            loading={membersLoading}
            error={membersError}
            connected={connected}
            currentUserId={user.id}
            canKick={canKick}
            kickingUserId={kickingUserId}
            kickDisabled={bansLoading || Boolean(bansError)}
            kickError={kickError || bansError}
            bannedUserIds={bannedUserIds}
            onKick={kickMember}
            currentUser={user}
            onClose={() => setShowMemberPanel(false)}
            onReload={refreshMembers}
            onOpenProfile={openMemberProfile}
          />
        )}

        {canKick && showBanPanel && (
          <ChatBanPanel
            key={roomId}
            roomId={Number(roomId)}
            bans={bans}
            loading={bansLoading}
            error={bansError}
            onClose={() => setShowBanPanel(false)}
            onReleased={(banId) => {
              setBans((current) =>
                current.filter((ban) => ban.banId !== banId),
              );
            }}
            onReload={() => {
              setBansLoading(true);
              setBanRevision((current) => current + 1);
            }}
          />
        )}
        {reportTarget && (
          <ChatReportModal
            key={`${roomId}:${reportTarget.messageId}`}
            roomId={roomId}
            message={reportTarget}
            onClose={() => setReportTarget(null)}
          />
        )}
        {profileTarget && (
          <UserFollowDialog
            key={profileTarget.userId}
            target={profileTarget}
            currentUser={user}
            onClose={() => setProfileTarget(null)}
          />
        )}
        <div className="chat-message-area">
          <div
            ref={messageListRef}
            className="chat-message-list"
            aria-label="채팅 메시지"
            tabIndex={0}
            onScroll={handleMessageScroll}
          >
            {!loading && hasNext && (
              <div className="chat-history-action">
                <Button
                  variant="outline"
                  onClick={loadOlderMessages}
                  disabled={loadingOlder}
                >
                  {loadingOlder ? "불러오는 중..." : "이전 메시지 불러오기"}
                </Button>
              </div>
            )}

            {!loading && !error && messages.length === 0 && (
              <p className="chat-empty">아직 대화가 없습니다.</p>
            )}

            {messages.map((message) => {
              const blocked = message.visibility !== "SHOW";
              const system = message.messageType !== "CHAT";
              const mine = String(message.userId) === String(user.id);
              const isRoomOwner =
                room?.createdById != null &&
                String(message.userId) === String(room.createdById);

              if (system) {
                return (
                  <div
                    key={message.messageId}
                    data-message-id={message.messageId}
                    className="chat-system-message"
                  >
                    {blocked ? "공개되지 않은 알림입니다." : message.content}
                  </div>
                );
              }

              return (
                <article
                  key={message.messageId}
                  data-message-id={message.messageId}
                  data-protected={blocked || undefined}
                  className={`chat-message ${mine ? "mine" : "other"}${blocked ? " is-blocked" : ""}`}
                >
                  <div className="chat-message-header">
                    <div className="chat-author">
                      {mine ? (
                        <strong>{message.nickname}</strong>
                      ) : (
                        <button
                          type="button"
                          className="chat-author-button"
                          onClick={() => {
                            setProfileTarget({
                              userId: message.userId,
                              nickname: message.nickname,
                            });
                          }}
                        >
                          {message.nickname}
                        </button>
                      )}

                      {onlineUserIds.has(String(message.userId)) && (
                        <span
                          className="chat-online-dot"
                          role="img"
                          aria-label="현재 이 채팅방에 접속 중"
                          title="현재 이 채팅방에 접속 중"
                        />
                      )}
                    </div>
                    {room && !mine && !isRoomOwner && (
                      <button
                        type="button"
                        className="chat-report-trigger"
                        aria-haspopup="dialog"
                        aria-label={`${message.nickname}님의 메시지 신고`}
                        onClick={() => {
                          setShowBanPanel(false);
                          setShowMemberPanel(false);
                          setReportTarget({
                            messageId: message.messageId,
                            nickname: message.nickname,
                          });
                        }}
                      >
                        신고
                      </button>
                    )}
                    {canKick &&
                      !mine &&
                      (bannedUserIds.has(String(message.userId)) ? (
                        <span className="chat-kicked-label">강퇴됨</span>
                      ) : (
                        <button
                          type="button"
                          className="chat-kick-button"
                          onClick={() =>
                            kickMember(message.userId, message.nickname)
                          }
                          disabled={
                            !connected ||
                            kickingUserId !== null ||
                            bansLoading ||
                            Boolean(bansError)
                          }
                        >
                          {String(kickingUserId) === String(message.userId)
                            ? "처리 중…"
                            : "강퇴"}
                        </button>
                      ))}
                  </div>

                  {blocked ? (
                    <ChatMessageGuard reason={message.blockReason} />
                  ) : (
                    <p className="chat-message-content">{message.content}</p>
                  )}

                  <time
                    dateTime={message.createdAt}
                    className="chat-message-time"
                  >
                    {new Date(message.createdAt).toLocaleTimeString("ko-KR", {
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </time>
                </article>
              );
            })}
          </div>
          {isAwayFromBottom && (
            <div className="chat-scroll-controls">
              {newMessageCount > 0 && (
                <button
                  type="button"
                  className="chat-new-message-notice"
                  onClick={goToBottom}
                >
                  <span role="status">새 메시지 {newMessageCount}개</span>
                  <span aria-hidden="true">↓</span>
                </button>
              )}

              <button
                type="button"
                className="chat-scroll-bottom"
                onClick={goToBottom}
                aria-label="최신 메시지로 이동"
                title="최신 메시지로 이동"
              >
                <svg
                  viewBox="0 0 24 24"
                  width="22"
                  height="22"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  aria-hidden="true"
                >
                  <path d="m6 9 6 6 6-6" />
                </svg>
              </button>
            </div>
          )}
        </div>

        <form onSubmit={sendMessage} className="chat-input-form">
          <input
            className="chat-input"
            aria-label="채팅 메시지"
            placeholder={
              connected || reconnecting
                ? "메시지를 입력하세요"
                : "채팅 연결을 확인해 주세요"
            }
            value={content}
            onChange={(event) => setContent(event.target.value)}
            maxLength={2000}
            disabled={leaving || (!connected && !reconnecting)}
          />

          <Button
            type="submit"
            variant="primary"
            disabled={!connected || leaving || !content.trim()}
          >
            전송
          </Button>
        </form>
      </section>
    </main>
  );
}
