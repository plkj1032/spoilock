import { useEffect, useState } from "react";
import { apiRequest } from "../api/authApi";
import { termsApi } from "../api/termsApi";

export default function KakaoSignupPage() {
  const [info, setInfo] = useState(null);
  const [terms, setTerms] = useState([]);
  const [nickname, setNickname] = useState("");
  const [checkedNickname, setCheckedNickname] = useState("");
  const [acceptedIds, setAcceptedIds] = useState([]);

  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [checking, setChecking] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    let active = true;

    Promise.all([
      apiRequest("/api/auth/kakao/pending", {
        cache: "no-store",
      }),
      termsApi.getCurrent(),
    ])
      .then(([pendingInfo, currentTerms]) => {
        if (!active) return;

        setInfo(pendingInfo);
        setTerms(Array.isArray(currentTerms) ? currentTerms : []);
      })
      .catch(() => {
        if (!active) return;

        setError(
          "카카오 가입 정보를 불러오지 못했습니다. " +
            "카카오 로그인을 다시 진행해 주세요.",
        );
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const consentTerms = terms.filter(
    (term) => term.termsType !== "PRIVACY_NOTICE",
  );

  const privacyTerms = terms.filter(
    (term) => term.termsType === "PRIVACY_NOTICE",
  );

  const requiredTerms = consentTerms.filter((term) => term.required);

  const hasServiceTerms = terms.some((term) => term.termsType === "TERMS");

  const requiredAccepted =
    hasServiceTerms &&
    requiredTerms.every((term) => acceptedIds.includes(term.id));

  const nicknameReady =
    info?.existing ||
    (nickname.trim().length >= 2 && checkedNickname === nickname.trim());

  const canSubmit =
    !loading &&
    !busy &&
    !checking &&
    Boolean(info) &&
    requiredAccepted &&
    nicknameReady;

  function toggleTerm(id, checked) {
    setAcceptedIds((current) =>
      checked
        ? [...new Set([...current, id])]
        : current.filter((value) => value !== id),
    );
  }

  function toggleAll(checked) {
    setAcceptedIds(checked ? consentTerms.map((term) => term.id) : []);
  }

  async function checkNickname() {
    const clean = nickname.trim();

    if (!clean) {
      setError("닉네임을 입력해 주세요.");
      return;
    }

    setChecking(true);
    setError("");
    setMessage("");

    try {
      const response = await apiRequest(
        "/api/auth/nickname/check?nickname=" + encodeURIComponent(clean),
        { cache: "no-store" },
      );

      if (!response?.available) {
        setCheckedNickname("");
        setError("이미 사용 중인 닉네임입니다.");
        return;
      }

      setCheckedNickname(clean);
      setMessage("사용 가능한 닉네임입니다.");
    } catch (err) {
      setCheckedNickname("");
      setError(err.message || "닉네임을 확인하지 못했습니다.");
    } finally {
      setChecking(false);
    }
  }

  async function completeSignup(event) {
    event.preventDefault();

    if (!canSubmit) return;

    setBusy(true);
    setError("");

    try {
      await apiRequest("/api/auth/kakao/complete", {
        method: "POST",
        body: {
          nickname: info.existing ? null : nickname.trim(),
          acceptedTermsIds: acceptedIds,
        },
      });

      // 새로운 인증 쿠키로 앱 전체 로그인 상태 복원
      window.location.replace("/");
    } catch (err) {
      setError(
        (err.message || "가입 완료에 실패했습니다.") +
          " 다시 시도하려면 카카오 인증부터 진행해 주세요.",
      );
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <main className="container main-content">
        <p>카카오 가입 정보를 확인하고 있어요…</p>
      </main>
    );
  }

  return (
    <main className="container main-content">
      <section
        style={{
          maxWidth: 560,
          margin: "24px auto",
        }}
      >
        <h1>
          {info?.existing ? "약관 동의가 필요합니다" : "카카오 회원가입 완료"}
        </h1>

        <p className="page-subtitle">
          {info?.existing
            ? `${info.nickname}님, 서비스 이용을 위해 현재 필수 약관을 확인해 주세요.`
            : "닉네임을 설정하고 약관에 동의하면 SPOILOCK 가입이 완료됩니다."}
        </p>

        {!info ? (
          <p role="alert">{error || "가입 정보를 찾을 수 없습니다."}</p>
        ) : (
          <form
            onSubmit={completeSignup}
            style={{
              display: "grid",
              gap: 20,
              marginTop: 24,
            }}
          >
            {!info.existing && (
              <div style={{ display: "grid", gap: 8 }}>
                <label htmlFor="kakao-nickname">닉네임</label>

                <div
                  style={{
                    display: "flex",
                    gap: 8,
                  }}
                >
                  <input
                    id="kakao-nickname"
                    value={nickname}
                    maxLength={20}
                    placeholder="2~20자 닉네임"
                    onChange={(event) => {
                      setNickname(event.target.value);
                      setCheckedNickname("");
                      setMessage("");
                    }}
                    disabled={busy || checking}
                    style={{ flex: 1, minWidth: 0 }}
                  />

                  <button
                    type="button"
                    onClick={checkNickname}
                    disabled={busy || checking}
                    className="button"
                  >
                    {checking ? "확인 중…" : "중복 확인"}
                  </button>
                </div>

                <small>
                  한글, 영문, 숫자, 밑줄만 사용할 수 있으며 욕설·비하 표현은
                  사용할 수 없습니다.
                </small>
              </div>
            )}

            <section
              aria-label="약관 동의"
              style={{ display: "grid", gap: 12 }}
            >
              <h2>서비스 약관</h2>

              {!hasServiceTerms && (
                <p role="alert">
                  현재 게시된 이용약관이 없습니다. 관리자에게 문의해 주세요.
                </p>
              )}

              {hasServiceTerms && (
                <label>
                  <input
                    type="checkbox"
                    checked={
                      consentTerms.length > 0 &&
                      consentTerms.every((term) =>
                        acceptedIds.includes(term.id),
                      )
                    }
                    onChange={(event) => toggleAll(event.target.checked)}
                    disabled={busy}
                  />{" "}
                  전체 동의
                </label>
              )}

              {consentTerms.map((term) => (
                <div key={term.id} className="auth-terms-item">
                  <label>
                    <input
                      type="checkbox"
                      checked={acceptedIds.includes(term.id)}
                      disabled={busy}
                      onChange={(event) =>
                        toggleTerm(term.id, event.target.checked)
                      }
                    />{" "}
                    <strong>{term.required ? "[필수]" : "[선택]"}</strong>{" "}
                    {term.title} (v{term.version})
                  </label>

                  <details>
                    <summary>약관 내용 보기</summary>
                    <pre
                      style={{
                        whiteSpace: "pre-wrap",
                        overflowWrap: "anywhere",
                      }}
                    >
                      {term.content}
                    </pre>
                  </details>
                </div>
              ))}

              {privacyTerms.map((term) => (
                <div key={term.id} className="auth-terms-item">
                  <details>
                    <summary>
                      [안내] {term.title} (v{term.version})
                    </summary>

                    <pre
                      style={{
                        whiteSpace: "pre-wrap",
                        overflowWrap: "anywhere",
                      }}
                    >
                      {term.content}
                    </pre>
                  </details>
                </div>
              ))}

              <small>
                선택 약관에 동의하지 않아도 가입할 수 있습니다. 개인정보
                처리방침은 별도로 열람할 수 있습니다.
              </small>
            </section>

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
              className="button button-primary"
              disabled={!canSubmit}
            >
              {busy
                ? "처리 중…"
                : info.existing
                  ? "동의하고 로그인"
                  : "동의하고 회원가입 완료"}
            </button>
          </form>
        )}

        <p style={{ marginTop: 20 }}>
          <a href="/oauth2/authorization/kakao">카카오 인증 다시 하기</a>
        </p>
      </section>
    </main>
  );
}
