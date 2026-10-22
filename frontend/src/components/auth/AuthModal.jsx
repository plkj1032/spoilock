import { useEffect, useState } from "react";
import Brand from "../common/Brand";

export default function AuthModal({
  mode,
  setMode,
  onClose,
  onLogin,
  onSignup,
  busy,
  error,
  setError,
}) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [nickname, setNickname] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [message, setMessage] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const signup = mode === "signup";

  useEffect(() => {
    const onKey = (event) => {
      if (event.key === "Escape" && !busy) onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [busy, onClose]);

  async function submit(event) {
    event.preventDefault();

    setError("");
    setMessage("");

    if (signup) {
      // 1. 비밀번호 확인
      if (password !== confirmPassword) {
        setError("비밀번호가 일치하지 않습니다.");
        return;
      }

      // 2. 닉네임 검증
      const cleanNickname = nickname.trim();

      if (cleanNickname.length < 2 || cleanNickname.length > 20) {
        setError("닉네임은 2~20자로 입력해주세요.");
        return;
      }

      // 3. 실제 회원가입 요청
      const ok = await onSignup(email.trim(), password, cleanNickname);

      if (ok) {
        setPassword("");
        setConfirmPassword("");
        setMode("login");
        setMessage("가입이 완료되었습니다. 로그인해 주세요.");
      }
    } else {
      // 기존 로그인 기능
      await onLogin(email.trim(), password);
    }
  }

  function switchMode() {
    setMode(signup ? "login" : "signup");
    setError("");
    setMessage("");
  }

  return (
    <div
      className="modal-backdrop"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !busy) onClose();
      }}
    >
      <section
        className="auth-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="auth-title"
      >
        <button
          type="button"
          className="modal-close"
          aria-label="닫기"
          onClick={onClose}
          disabled={busy}
        >
          ×
        </button>
        <div className="dialog-brand">
          <Brand />
        </div>
        <h2 id="auth-title">
          {signup ? "SPOILOCK 시작하기" : "다시 만나서 반가워요"}
        </h2>
        <p className="dialog-description">
          {signup
            ? "안전한 드라마 커뮤니티에 참여하세요."
            : "로그인하고 안전한 대화를 이어가세요."}
        </p>
        <form onSubmit={submit} className="auth-form">
          <label htmlFor="auth-email">이메일</label>
          <input
            id="auth-email"
            type="email"
            autoComplete="email"
            placeholder="name@example.com"
            required
            maxLength={255}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <label htmlFor="auth-password">비밀번호</label>
          <div className="password-input">
            <input
              id="auth-password"
              type={showPassword ? "text" : "password"}
              autoComplete={signup ? "new-password" : "current-password"}
              placeholder="8자 이상 입력해 주세요"
              minLength={signup ? 8 : undefined}
              maxLength={signup ? 64 : undefined}
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <button
              type="button"
              className="password-toggle"
              onClick={() => setShowPassword((s) => !s)}
              aria-label={showPassword ? "비밀번호 숨기기" : "비밀번호 표시"}
            >
              {showPassword ? "숨김" : "보기"}
            </button>
          </div>
          {signup && (
            <>
              <label htmlFor="auth-confirm-password">비밀번호 확인</label>
              <input
                id="auth-confirm-password"
                type={showPassword ? "text" : "password"}
                autoComplete="new-password"
                placeholder="비밀번호를 다시 입력해 주세요"
                required
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
              <label htmlFor="auth-nickname">닉네임</label>
              <input
                id="auth-nickname"
                type="text"
                placeholder="사용할 닉네임을 입력해 주세요"
                required
                maxLength={50}
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
              />
            </>
          )}
          {message && (
            <p className="form-success" role="status">
              {message}
            </p>
          )}
          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}
          <button
            type="submit"
            className="button button-primary submit-button"
            disabled={busy}
          >
            {busy ? "처리 중..." : signup ? "회원가입" : "로그인"}
          </button>
        </form>
        <p className="switch-auth">
          {signup ? "이미 계정이 있나요?" : "아직 회원이 아니신가요?"}{" "}
          <button type="button" onClick={switchMode} disabled={busy}>
            {signup ? "로그인" : "회원가입"}
          </button>
        </p>
        <button
          type="button"
          className="button button-outline submit-button"
          disabled={busy}
          onClick={() => {
            window.location.assign(
              "http://localhost:8080/oauth2/authorization/kakao",
            );
          }}
        >
          카카오로 로그인
        </button>
      </section>
    </div>
  );
}
