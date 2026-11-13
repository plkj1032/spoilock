// STOMP 연결과 실제 WebSocket 연결 상태를 함께 확인
export function canPublish(client) {
  return Boolean(
    client?.active &&
    client.connected &&
    client.webSocket?.readyState === WebSocket.OPEN,
  );
}

// 인증 확인 실패로 중단된 연결을 주기적으로 복구
export function installConnectionRecovery(client) {
  let disposed = false;
  let nextAttemptAt = Date.now() + 30000;

  function recover() {
    if (
      disposed ||
      document.visibilityState !== "visible" ||
      !navigator.onLine ||
      client.active ||
      Date.now() < nextAttemptAt
    ) {
      return;
    }

    nextAttemptAt = Date.now() + 30000;

    // 기존 beforeConnect에서 인증 확인 후 실제 연결
    client.activate();
  }

  const timer = window.setInterval(recover, 30000);

  window.addEventListener("focus", recover);
  window.addEventListener("online", recover);
  window.addEventListener("pageshow", recover);
  document.addEventListener("visibilitychange", recover);

  return () => {
    disposed = true;
    window.clearInterval(timer);

    window.removeEventListener("focus", recover);
    window.removeEventListener("online", recover);
    window.removeEventListener("pageshow", recover);
    document.removeEventListener("visibilitychange", recover);
  };
}
export function deactivateSafely(client) {
  if (!client) return Promise.resolve();

  const socket = client.webSocket;

  // 실제 연결이 열려 있으면 정상 종료 절차 사용
  // 이미 닫히고 있거나 연결이 없으면 DISCONNECT 전송 생략
  const force = !socket || socket.readyState !== WebSocket.OPEN;

  return client.deactivate({ force });
}
