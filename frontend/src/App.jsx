import { useEffect, useState, useCallback, useRef } from "react";
import { authApi } from "./api/authApi";

import Header from "./components/common/Header";
import AuthModal from "./components/auth/AuthModal";
import AppRouter from "./router/AppRouter";
import Toast from "./components/common/Toast";

import "./App.css";

function App() {
  const [user, setUser] = useState(null);
  const signedInUserId = user?.id;

  const [pending, setPending] = useState(true);
  const [busy, setBusy] = useState(false);
  const [mode, setMode] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const sessionExpiredHandledRef = useRef(false);

  const handleSessionExpired = useCallback(() => {
    // 이미 로그아웃을 처리했다면 중복 실행 방지
    if (sessionExpiredHandledRef.current) {
      return;
    }

    sessionExpiredHandledRef.current = true;

    setUser(null);

    setNotice("다른 브라우저에서 로그인했거나 세션이 만료되었습니다.");
  }, []);

  // 새로운 로그인에 성공하면 다시 감지할 수 있도록 초기화
  useEffect(() => {
    if (signedInUserId) {
      sessionExpiredHandledRef.current = false;
    }
  }, [signedInUserId]);

  // 토스트 메시지가 표시되면 5초 후 자동으로 닫기
  useEffect(() => {
    if (!notice) return;

    const timer = window.setTimeout(() => {
      setNotice("");
    }, 5000);

    return () => {
      window.clearTimeout(timer);
    };
  }, [notice]);

  // 새로고침 시 로그인 상태 복구
  useEffect(() => {
    let active = true;

    authApi
      .restore()
      .then((me) => {
        if (active) setUser(me);
      })
      .catch(() => {
        if (active) {
          setNotice("백엔드 연결 상태를 확인해 주세요.");
        }
      })
      .finally(() => {
        if (active) setPending(false);
      });

    return () => {
      active = false;
    };
  }, []);

  // 단일 기기 로그인 감지 useEffect

  useEffect(() => {
    if (!signedInUserId) return;

    let stopped = false;
    let checking = false;

    const checkSession = async () => {
      if (stopped || checking || document.visibilityState === "hidden") {
        return;
      }

      checking = true;

      try {
        const me = await authApi.restore();

        if (!stopped && me === null) {
          handleSessionExpired();
        }
      } catch {
        // 일시적인 네트워크 오류는 로그아웃하지 않음
      } finally {
        checking = false;
      }
    };

    // 다른 탭에서 복귀했을 때
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        void checkSession();
      }
    };

    // 브라우저 창에 포커스가 돌아왔을 때
    const handleFocus = () => {
      void checkSession();
    };

    // 네트워크 연결이 복구됐을 때
    const handleOnline = () => {
      void checkSession();
    };

    document.addEventListener("visibilitychange", handleVisibilityChange);

    window.addEventListener("focus", handleFocus);
    window.addEventListener("online", handleOnline);

    return () => {
      stopped = true;

      document.removeEventListener("visibilitychange", handleVisibilityChange);

      window.removeEventListener("focus", handleFocus);
      window.removeEventListener("online", handleOnline);
    };
  }, [signedInUserId, handleSessionExpired]);

  // 보호 API에서 세션 만료가 확정되면 로그아웃 처리

  // 보호 API에서 세션 만료가 확정되면 로그아웃 처리
  useEffect(() => {
    // 로그인하지 않은 상태에서는 이벤트 처리하지 않음
    if (!signedInUserId) return;

    // 위에서 선언한 공통 함수를 그대로 사용
    window.addEventListener("spoilock:session-expired", handleSessionExpired);

    return () => {
      window.removeEventListener(
        "spoilock:session-expired",
        handleSessionExpired,
      );
    };
  }, [signedInUserId, handleSessionExpired]);

  function openModal(which = "login") {
    setMode(which);
    setError("");
  }

  function closeModal() {
    if (!busy) {
      setMode(null);
      setError("");
    }
  }

  async function handleLogin(email, password) {
    setBusy(true);
    setError("");

    try {
      const me = await authApi.login(email, password);

      sessionExpiredHandledRef.current = false;

      setUser(me);
      setMode(null);
      setNotice(`${me.nickname}님, 환영합니다!`);

      return true;
    } catch (err) {
      setError(err.message);
      return false;
    } finally {
      setBusy(false);
    }
  }

  async function handleSignup(
    email,
    password,
    nickname,
    signupProof,
    acceptedTermsIds,
  ) {
    setBusy(true);
    setError("");

    try {
      await authApi.signup(
        email,
        password,
        nickname,
        signupProof,
        acceptedTermsIds,
      );

      return true;
    } catch (err) {
      setError(err.message);
      return false;
    } finally {
      setBusy(false);
    }
  }

  async function handleLogout() {
    setBusy(true);

    try {
      await authApi.logout();

      setUser(null);
      setNotice("로그아웃되었습니다.");
    } catch (err) {
      setNotice(`로그아웃 실패: ${err.message}`);
    } finally {
      setBusy(false);
    }
  }
  function handleProfileUpdated(profile) {
    setUser((current) => {
      if (!current || String(current.id) !== String(profile.id)) {
        return current;
      }

      return { ...current, ...profile };
    });
  }

  return (
    <>
      <Header
        user={user}
        pending={pending}
        busy={busy}
        onLogin={() => openModal("login")}
        onLogout={handleLogout}
      />

      <AppRouter
        user={user}
        pending={pending}
        onLogin={() => openModal("login")}
        onNotice={setNotice}
        onProfileUpdated={handleProfileUpdated}
      />

      <Toast message={notice} onClose={() => setNotice("")} />

      {mode && (
        <AuthModal
          mode={mode}
          setMode={setMode}
          onClose={closeModal}
          onLogin={handleLogin}
          onSignup={handleSignup}
          busy={busy}
          error={error}
          setError={setError}
        />
      )}
    </>
  );
}

export default App;
