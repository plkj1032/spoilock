import { useEffect, useState } from "react";

import { authApi } from "../../api/authApi";
import { termsApi } from "../../api/termsApi";

import Brand from "../common/Brand";
import Button from "../common/Button";

import "../../styles/AuthModal.css";
import "../../styles/Modal.css";

function normalizeEmail(value) {
  return value.trim().toLowerCase();
}

function normalizeNickname(value) {
  return value.trim().toLowerCase();
}

const TERMS_LABELS = {
  TERMS: "서비스 이용약관",
  PRIVACY_NOTICE: "개인정보 처리방침",
  MARKETING: "마케팅 정보 수신",
};

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
  const signup = mode === "signup";

  // ==========================================
  // 1. 기본 입력
  // ==========================================
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [nickname, setNickname] = useState("");

  const [showPassword, setShowPassword] = useState(false);
  const [message, setMessage] = useState("");

  // ==========================================
  // 2. 이메일 인증
  // ==========================================
  const [emailCode, setEmailCode] = useState("");
  const [emailRequested, setEmailRequested] = useState(false);
  const [requestedEmail, setRequestedEmail] = useState("");
  const [verifiedEmail, setVerifiedEmail] = useState("");
  const [signupProof, setSignupProof] = useState("");

  const [resendSeconds, setResendSeconds] = useState(0);

  // ==========================================
  // 3. 닉네임 중복 확인
  // ==========================================
  const [checkedNickname, setCheckedNickname] = useState("");

  // ==========================================
  // 4. 약관 관리
  // ==========================================
  const [terms, setTerms] = useState([]);
  const [acceptedTermsIds, setAcceptedTermsIds] = useState([]);

  const [termsLoading, setTermsLoading] = useState(false);
  const [termsError, setTermsError] = useState("");
  const [termsReloadKey, setTermsReloadKey] = useState(0);

  // 상세 내용을 펼친 약관 ID
  const [expandedTermId, setExpandedTermId] = useState(null);

  // ==========================================
  // 5. API 요청 상태
  // ==========================================
  const [emailBusy, setEmailBusy] = useState(false);
  const [verifyBusy, setVerifyBusy] = useState(false);
  const [nicknameBusy, setNicknameBusy] = useState(false);

  const actionBusy = busy || emailBusy || verifyBusy || nicknameBusy;

  const currentEmail = normalizeEmail(email);
  const currentNickname = normalizeNickname(nickname);

  const emailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(currentEmail);

  const emailVerified = Boolean(signupProof) && verifiedEmail === currentEmail;

  // 비밀번호 확인 입력 여부
  const passwordConfirmEntered = confirmPassword.length > 0;

  // 비밀번호 일치 여부
  const passwordsMatch =
    password.length > 0 &&
    passwordConfirmEntered &&
    password === confirmPassword;

  const nicknameAvailable =
    Boolean(checkedNickname) && checkedNickname === currentNickname;

  // 개인정보 처리방침은 고지/열람 대상으로 표시
  // 이용약관과 마케팅 동의는 체크박스 표시
  const consentTerms = terms.filter(
    (term) => term.termsType !== "PRIVACY_NOTICE",
  );

  const privacyTerms = terms.filter(
    (term) => term.termsType === "PRIVACY_NOTICE",
  );

  const requiredTerms = consentTerms.filter((term) => term.required);

  const hasPublishedServiceTerms = terms.some(
    (term) => term.termsType === "TERMS",
  );

  const allRequiredAccepted =
    hasPublishedServiceTerms &&
    requiredTerms.every((term) => acceptedTermsIds.includes(term.id));

  const termsReady = !termsLoading && !termsError && hasPublishedServiceTerms;

  // ==========================================
  // 6. ESC 모달 닫기
  // ==========================================
  useEffect(() => {
    function onKey(event) {
      if (event.key === "Escape" && !actionBusy) {
        onClose();
      }
    }

    window.addEventListener("keydown", onKey);

    return () => {
      window.removeEventListener("keydown", onKey);
    };
  }, [actionBusy, onClose]);

  // ==========================================
  // 7. 이메일 재발송 카운트다운
  // ==========================================
  useEffect(() => {
    if (resendSeconds <= 0) return;

    const timer = window.setTimeout(() => {
      setResendSeconds((current) => Math.max(current - 1, 0));
    }, 1000);

    return () => {
      window.clearTimeout(timer);
    };
  }, [resendSeconds]);

  // ==========================================
  // 회원가입 화면 진입 시 약관 조회
  // ==========================================
  useEffect(() => {
    if (!signup) return;

    let active = true;

    // React Effect에서는 API 요청을 시작하고
    // 응답이 도착한 뒤 상태를 업데이트합니다.
    termsApi
      .getCurrent()
      .then((data) => {
        if (!active) return;

        const list = Array.isArray(data) ? data : [];

        setTerms(list);
        setTermsError("");
        setAcceptedTermsIds([]);

        const hasServiceTerms = list.some((term) => term.termsType === "TERMS");

        if (!hasServiceTerms) {
          setTermsError(
            "현재 게시된 서비스 이용약관이 없습니다. 관리자에게 문의해 주세요.",
          );
        }
      })
      .catch((err) => {
        if (!active) return;

        setTerms([]);
        setTermsError(err.message || "약관을 불러오지 못했습니다.");
      })
      .finally(() => {
        if (active) {
          setTermsLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [signup, termsReloadKey]);

  // ==========================================
  // 9. 이메일 변경
  // ==========================================
  function handleEmailChange(event) {
    setEmail(event.target.value);

    setEmailCode("");
    setEmailRequested(false);
    setRequestedEmail("");
    setVerifiedEmail("");
    setSignupProof("");
    setResendSeconds(0);

    setError("");
    setMessage("");
  }

  // ==========================================
  // 10. 인증번호 발송 / 재발송
  // ==========================================
  async function handleSendEmailCode() {
    if (actionBusy || resendSeconds > 0) return;

    if (!emailValid) {
      setError("올바른 이메일을 입력해 주세요.");
      return;
    }

    setEmailBusy(true);
    setError("");
    setMessage("");

    try {
      await authApi.sendEmailCode(currentEmail);

      setEmailRequested(true);
      setRequestedEmail(currentEmail);
      setEmailCode("");
      setVerifiedEmail("");
      setSignupProof("");

      // 서버의 최소 60초 재발송 제한에 맞춤
      setResendSeconds(60);

      setMessage(
        "가입 가능한 이메일이라면 인증번호가 발송됩니다. 받은편지함과 스팸함을 확인해 주세요.",
      );
    } catch (err) {
      setError(err.message);
    } finally {
      setEmailBusy(false);
    }
  }

  // ==========================================
  // 11. 이메일 인증번호 확인
  // ==========================================
  async function handleVerifyEmailCode() {
    if (actionBusy) return;

    if (!emailRequested || requestedEmail !== currentEmail) {
      setError("인증번호를 먼저 요청해 주세요.");
      return;
    }

    if (!/^[0-9]{6}$/.test(emailCode)) {
      setError("인증번호 6자리를 입력해 주세요.");
      return;
    }

    setVerifyBusy(true);
    setError("");
    setMessage("");

    try {
      const result = await authApi.verifyEmailCode(currentEmail, emailCode);

      if (!result?.signupProof) {
        throw new Error("인증 결과를 확인할 수 없습니다.");
      }

      // 이메일 인증 성공
      setSignupProof(result.signupProof);
      setVerifiedEmail(currentEmail);

      // 인증 완료 후 재발송 카운트다운 초기화
      setResendSeconds(0);

      // 인증번호 입력값 초기화
      setEmailCode("");

      setMessage("이메일 인증이 완료되었습니다.");
    } catch (err) {
      setError(err.message);
    } finally {
      setVerifyBusy(false);
    }
  }

  // ==========================================
  // 12. 닉네임 변경
  // ==========================================
  function handleNicknameChange(event) {
    setNickname(event.target.value);
    setCheckedNickname("");

    setError("");
    setMessage("");
  }

  // ==========================================
  // 13. 닉네임 중복 확인
  // ==========================================
  async function handleCheckNickname() {
    if (actionBusy) return;

    const cleanNickname = nickname.trim();

    if (cleanNickname.length < 2 || cleanNickname.length > 20) {
      setError("닉네임은 2~20자로 입력해 주세요.");
      return;
    }

    setNicknameBusy(true);
    setError("");
    setMessage("");

    try {
      const result = await authApi.checkNickname(cleanNickname);

      if (!result?.available) {
        setCheckedNickname("");
        setError("이미 사용 중인 닉네임입니다.");
        return;
      }

      setCheckedNickname(normalizeNickname(cleanNickname));

      setMessage("사용 가능한 닉네임입니다.");
    } catch (err) {
      setError(err.message);
    } finally {
      setNicknameBusy(false);
    }
  }

  // ==========================================
  // 14. 약관 체크 / 해제
  // ==========================================
  function toggleTerm(termId, checked) {
    setAcceptedTermsIds((current) => {
      if (checked) {
        return current.includes(termId) ? current : [...current, termId];
      }

      return current.filter((id) => id !== termId);
    });
  }

  // ==========================================
  // 15. 동의 가능한 약관 모두 체크
  // ==========================================
  function toggleAllTerms(checked) {
    if (checked) {
      setAcceptedTermsIds(consentTerms.map((term) => term.id));
    } else {
      setAcceptedTermsIds([]);
    }
  }

  const allTermsAccepted =
    consentTerms.length > 0 &&
    consentTerms.every((term) => acceptedTermsIds.includes(term.id));

  // ==========================================
  // 16. 회원가입 / 로그인
  // ==========================================
  async function submit(event) {
    event.preventDefault();

    if (actionBusy) return;

    setError("");
    setMessage("");

    // 일반 로그인은 기존 로직 유지
    if (!signup) {
      await onLogin(currentEmail, password);
      return;
    }

    if (!emailVerified) {
      setError("이메일 인증을 완료해 주세요.");
      return;
    }

    if (password.length < 8 || password.length > 64) {
      setError("비밀번호는 8~64자로 입력해 주세요.");
      return;
    }

    // BCrypt 사용 시 입력 바이트 길이 제한
    if (new TextEncoder().encode(password).length > 72) {
      setError("비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.");
      return;
    }

    if (password !== confirmPassword) {
      setError("비밀번호가 일치하지 않습니다.");
      return;
    }

    if (!nicknameAvailable) {
      setError("닉네임 중복 확인을 완료해 주세요.");
      return;
    }

    if (!termsReady) {
      setError("약관을 불러온 후 다시 시도해 주세요.");
      return;
    }

    if (!allRequiredAccepted) {
      setError("필수 이용약관에 동의해 주세요.");
      return;
    }

    // ========================================
    // 이메일 증명 + 약관 동의 ID 함께 전달
    // ========================================
    const ok = await onSignup(
      currentEmail,
      password,
      nickname.trim(),
      signupProof,
      acceptedTermsIds,
    );

    if (ok) {
      setPassword("");
      setConfirmPassword("");

      setSignupProof("");
      setVerifiedEmail("");
      setEmailRequested(false);
      setRequestedEmail("");
      setEmailCode("");
      setResendSeconds(0);

      setCheckedNickname("");
      setAcceptedTermsIds([]);

      setMode("login");

      setMessage("가입이 완료되었습니다. 로그인해 주세요.");
    }
  }

  // ==========================================
  // 17. 로그인 / 회원가입 전환
  // ==========================================
  function switchMode() {
    if (actionBusy) return;

    setMode(signup ? "login" : "signup");

    setError("");
    setMessage("");

    setEmailRequested(false);
    setRequestedEmail("");
    setEmailCode("");
    setSignupProof("");
    setVerifiedEmail("");
    setResendSeconds(0);
    setCheckedNickname("");

    setAcceptedTermsIds([]);
    setExpandedTermId(null);
  }

  // ==========================================
  // 18. JSX
  // ==========================================
  return (
    <div
      className="modal-backdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !actionBusy) {
          onClose();
        }
      }}
    >
      <section
        className="auth-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="auth-title"
      >
        {/* 닫기 */}
        <button
          type="button"
          className="modal-close"
          aria-label="닫기"
          disabled={actionBusy}
          onClick={onClose}
        >
          ×
        </button>

        {/* 브랜드 */}
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

        <form className="auth-form" onSubmit={submit}>
          {/* =============================
              이메일
          ============================= */}
          <label htmlFor="auth-email">이메일</label>

          <div className={signup ? "auth-inline-row" : ""}>
            <input
              id="auth-email"
              type="email"
              autoComplete="email"
              placeholder="name@example.com"
              required
              maxLength={255}
              value={email}
              onChange={handleEmailChange}
              disabled={actionBusy}
            />

            {signup && (
              <button
                type="button"
                className="auth-inline-button"
                onClick={handleSendEmailCode}
                disabled={
                  actionBusy ||
                  emailVerified ||
                  resendSeconds > 0 ||
                  !emailValid
                }
              >
                {emailBusy
                  ? "발송 중..."
                  : emailVerified
                    ? "인증 완료"
                    : resendSeconds > 0
                      ? `${resendSeconds}초`
                      : emailRequested
                        ? "재발송"
                        : "인증번호 받기"}
              </button>
            )}
          </div>

          {/* =============================
              이메일 인증
          ============================= */}
          {signup && (
            <>
              {emailVerified ? (
                <p className="auth-verified-message" role="status">
                  ✓ 이메일 인증 완료
                </p>
              ) : (
                emailRequested && (
                  <>
                    <label htmlFor="auth-email-code">이메일 인증번호</label>

                    <div className="auth-inline-row">
                      <input
                        id="auth-email-code"
                        type="text"
                        inputMode="numeric"
                        autoComplete="one-time-code"
                        maxLength={6}
                        placeholder="6자리 인증번호"
                        value={emailCode}
                        disabled={actionBusy}
                        onChange={(event) =>
                          setEmailCode(event.target.value.replace(/\D/g, ""))
                        }
                      />

                      <button
                        type="button"
                        className="auth-inline-button"
                        disabled={actionBusy || !/^[0-9]{6}$/.test(emailCode)}
                        onClick={handleVerifyEmailCode}
                      >
                        {verifyBusy ? "확인 중..." : "인증 확인"}
                      </button>
                    </div>

                    <p className="auth-helper-text">
                      인증번호는 10분간 유효합니다. 재발송은 60초 후 가능합니다.
                    </p>
                  </>
                )
              )}
            </>
          )}

          {/* =============================
              비밀번호
          ============================= */}
          <label htmlFor="auth-password">비밀번호</label>

          <div className="password-input">
            <input
              id="auth-password"
              type={showPassword ? "text" : "password"}
              autoComplete={signup ? "new-password" : "current-password"}
              placeholder={signup ? "8~64자 비밀번호" : "비밀번호 입력"}
              minLength={signup ? 8 : undefined}
              maxLength={signup ? 64 : undefined}
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              disabled={actionBusy}
            />

            <button
              type="button"
              className="password-toggle"
              onClick={() => setShowPassword((current) => !current)}
              aria-label={showPassword ? "비밀번호 숨기기" : "비밀번호 표시"}
            >
              {showPassword ? "숨김" : "보기"}
            </button>
          </div>

          {/* =============================
              회원가입 전용
          ============================= */}
          {signup && (
            <>
              <label htmlFor="auth-confirm-password">비밀번호 확인</label>

              <input
                id="auth-confirm-password"
                type={showPassword ? "text" : "password"}
                autoComplete="new-password"
                placeholder="비밀번호를 다시 입력해 주세요"
                minLength={8}
                maxLength={64}
                required
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                disabled={actionBusy}
                aria-invalid={passwordConfirmEntered && !passwordsMatch}
                aria-describedby={
                  passwordConfirmEntered ? "password-match-message" : undefined
                }
              />

              {passwordConfirmEntered && (
                <p
                  id="password-match-message"
                  className={
                    passwordsMatch ? "auth-verified-message" : "form-error"
                  }
                  role="status"
                >
                  {passwordsMatch
                    ? "✓ 비밀번호가 일치합니다."
                    : "비밀번호가 일치하지 않습니다."}
                </p>
              )}

              <label htmlFor="auth-nickname">닉네임</label>

              <div className="auth-inline-row">
                <input
                  id="auth-nickname"
                  type="text"
                  placeholder="2~20자 닉네임"
                  required
                  maxLength={20}
                  value={nickname}
                  onChange={handleNicknameChange}
                  disabled={actionBusy}
                />

                <button
                  type="button"
                  className="auth-inline-button"
                  disabled={actionBusy || nicknameAvailable}
                  onClick={handleCheckNickname}
                >
                  {nicknameBusy
                    ? "확인 중..."
                    : nicknameAvailable
                      ? "확인 완료"
                      : "중복 확인"}
                </button>
              </div>

              {nicknameAvailable && (
                <p className="auth-verified-message" role="status">
                  ✓ 사용 가능한 닉네임입니다.
                </p>
              )}

              {/* ===========================
                  약관 동의
              =========================== */}
              <div className="auth-terms-section">
                <h3>서비스 이용 동의</h3>

                {termsLoading && (
                  <p className="auth-helper-text" role="status">
                    약관을 불러오는 중입니다.
                  </p>
                )}

                {termsError && (
                  <div className="auth-terms-error" role="alert">
                    <p>{termsError}</p>

                    <button
                      type="button"
                      onClick={() =>
                        setTermsReloadKey((current) => current + 1)
                      }
                      disabled={actionBusy}
                    >
                      다시 불러오기
                    </button>
                  </div>
                )}

                {termsReady && (
                  <>
                    <label className="auth-terms-all">
                      <input
                        type="checkbox"
                        checked={allTermsAccepted}
                        disabled={actionBusy}
                        onChange={(event) =>
                          toggleAllTerms(event.target.checked)
                        }
                      />
                      <strong>전체 동의</strong>
                    </label>

                    <p className="auth-helper-text">
                      선택 동의는 체크하지 않아도 회원가입할 수 있습니다.
                    </p>

                    {/* 체크박스가 필요한 약관 */}
                    {consentTerms.map((term) => (
                      <div key={term.id} className="auth-terms-item">
                        <div className="auth-terms-row">
                          <label>
                            <input
                              type="checkbox"
                              checked={acceptedTermsIds.includes(term.id)}
                              disabled={actionBusy}
                              onChange={(event) =>
                                toggleTerm(term.id, event.target.checked)
                              }
                            />

                            <span>
                              <span
                                className={
                                  term.required
                                    ? "auth-term-required"
                                    : "auth-term-optional"
                                }
                              >
                                {term.required ? "[필수]" : "[선택]"}
                              </span>{" "}
                              {term.title || TERMS_LABELS[term.termsType]}
                              <small> v{term.version}</small>
                            </span>
                          </label>

                          <button
                            type="button"
                            className="auth-terms-view"
                            aria-expanded={expandedTermId === term.id}
                            onClick={() =>
                              setExpandedTermId(
                                expandedTermId === term.id ? null : term.id,
                              )
                            }
                          >
                            {expandedTermId === term.id ? "닫기" : "보기"}
                          </button>
                        </div>

                        {expandedTermId === term.id && (
                          <div className="auth-terms-content">
                            <pre>{term.content}</pre>
                          </div>
                        )}
                      </div>
                    ))}

                    {/* 개인정보 처리방침: 동의가 아닌 열람 */}
                    {privacyTerms.map((term) => (
                      <div key={term.id} className="auth-terms-item">
                        <div className="auth-terms-row">
                          <span>
                            [안내] {term.title}
                            <small> v{term.version}</small>
                          </span>

                          <button
                            type="button"
                            className="auth-terms-view"
                            aria-expanded={expandedTermId === term.id}
                            onClick={() =>
                              setExpandedTermId(
                                expandedTermId === term.id ? null : term.id,
                              )
                            }
                          >
                            {expandedTermId === term.id ? "닫기" : "보기"}
                          </button>
                        </div>

                        {expandedTermId === term.id && (
                          <div className="auth-terms-content">
                            <pre>{term.content}</pre>
                          </div>
                        )}
                      </div>
                    ))}

                    {!allRequiredAccepted && (
                      <p className="auth-helper-text">
                        필수 이용약관에 동의해야 회원가입이 가능합니다.
                      </p>
                    )}
                  </>
                )}
              </div>
            </>
          )}

          {/* =============================
              성공 / 실패 메시지
          ============================= */}
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

          {/* =============================
              회원가입 / 로그인
          ============================= */}
          <Button
            type="submit"
            variant="primary"
            className="submit-button"
            disabled={
              actionBusy ||
              (signup &&
                (!emailVerified ||
                  !passwordsMatch ||
                  !nicknameAvailable ||
                  !termsReady ||
                  !allRequiredAccepted))
            }
          >
            {busy ? "처리 중..." : signup ? "회원가입" : "로그인"}
          </Button>
        </form>

        {/* 카카오 로그인 유지 */}
        <Button
          variant="kakao"
          className="submit-button kakao-login-button"
          disabled={actionBusy}
          onClick={() => {
            window.location.assign("/oauth2/authorization/kakao");
          }}
        >
          카카오로 로그인
        </Button>

        <p className="switch-auth">
          {signup ? "이미 계정이 있나요?" : "아직 회원이 아니신가요?"}{" "}
          <button type="button" disabled={actionBusy} onClick={switchMode}>
            {signup ? "로그인" : "회원가입"}
          </button>
        </p>
      </section>
    </div>
  );
}
