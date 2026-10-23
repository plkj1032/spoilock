import { useEffect, useState } from "react";
import { Navigate, Route, Routes } from "react-router-dom";

import { authApi } from "./api/authApi";

import Header from "./components/common/Header";
import AuthModal from "./components/auth/AuthModal";
import AppRouter from "./router/AppRouter";

import "./App.css";

function App() {
  const [user, setUser] = useState(null);
  const [pending, setPending] = useState(true);
  const [busy, setBusy] = useState(false);
  const [mode, setMode] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

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

  async function handleSignup(email, password, nickname) {
    setBusy(true);
    setError("");

    try {
      await authApi.signup(email, password, nickname);
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
      />

      {notice && (
        <div className="toast" role="status">
          <span>{notice}</span>
          <button
            type="button"
            aria-label="알림 닫기"
            onClick={() => setNotice("")}
          >
            ×
          </button>
        </div>
      )}

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
