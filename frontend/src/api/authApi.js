// frontend/src/api/authApi.js

// 개발 환경에서는 Vite가 /api 요청을 Spring Boot(8080)로 전달합니다.
// access_token / refresh_token은 HttpOnly 쿠키로 관리합니다.

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

// =====================================================
// 1. 쿠키 조회 (CSRF 토큰용)
// =====================================================
function readCookie(name) {
  const prefix = `${name}=`;

  const cookie = document.cookie
    .split(";")
    .map((item) => item.trim())
    .find((item) => item.startsWith(prefix));

  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : null;
}

// =====================================================
// 2. CSRF 토큰 발급
// =====================================================
// 같은 탭에서 CSRF 토큰 발급 요청을 하나로 공유
let csrfInFlight = null;

async function csrfToken() {
  if (csrfInFlight) {
    await csrfInFlight;
  } else if (!readCookie("XSRF-TOKEN")) {
    csrfInFlight = (async () => {
      const response = await fetch("/api/auth/csrf", {
        method: "GET",
        credentials: "include",
        cache: "no-store",
      });

      if (!response.ok) {
        throw new ApiError(response.status, "CSRF 토큰을 가져오지 못했습니다.");
      }
    })();

    try {
      await csrfInFlight;
    } finally {
      csrfInFlight = null;
    }
  }

  // 발급 응답의 값 대신 현재 쿠키를 읽음
  const token = readCookie("XSRF-TOKEN");

  if (!token) {
    throw new Error(
      "보안 토큰을 확인하지 못했습니다. 페이지를 새로고침해 주세요.",
    );
  }

  return token;
}

// =====================================================
// 3. 공통 API 요청 함수
// JSON + FormData + signal + cache 통합
// =====================================================
async function request(
  path,
  { method = "GET", body, formData, signal, cache, responseType = "json" } = {},
) {
  // JSON과 FormData 동시 전송 방지
  if (body !== undefined && formData !== undefined) {
    throw new Error("JSON과 FormData를 동시에 보낼 수 없습니다.");
  }

  const headers = {};
  const requestMethod = method.toUpperCase();

  // JSON 요청에만 Content-Type 설정
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  // JSON 또는 FormData 요청 본문 생성
  const requestBody =
    formData !== undefined
      ? formData
      : body !== undefined
        ? JSON.stringify(body)
        : undefined;

  const needsCsrf = !["GET", "HEAD", "OPTIONS"].includes(requestMethod);

  if (needsCsrf) {
    // 쿠키가 없다면 발급 완료까지 기다림
    await csrfToken();
  }

  // 인증·토큰 확인을 기다리는 동안 취소된 요청은 전송하지 않음
  if (signal?.aborted) {
    throw new DOMException("요청이 취소되었습니다.", "AbortError");
  }

  if (needsCsrf) {
    // await 이후 현재 쿠키를 다시 읽고 즉시 요청
    const token = readCookie("XSRF-TOKEN");

    if (!token) {
      throw new Error(
        "보안 토큰이 변경되었습니다. 잠시 후 다시 시도해 주세요.",
      );
    }

    headers["X-XSRF-TOKEN"] = token;
  }
  // 실제 HTTP 요청
  const response = await fetch(path, {
    method: requestMethod,
    headers,
    credentials: "include",
    signal,
    cache,
    ...(requestBody !== undefined ? { body: requestBody } : {}),
  });

  // 응답 처리
  const contentType = response.headers.get("content-type") || "";

  let data = null;

  if (response.status !== 204 && contentType.includes("application/json")) {
    data = await response.json().catch(() => null);
  }

  // HTTP 오류 처리
  if (!response.ok) {
    const messages = {
      400: "입력값 또는 파일 형식을 확인해 주세요.",
      401: "로그인이 필요하거나 인증 정보가 만료되었습니다.",
      403: "요청이 거부되었습니다. 권한 또는 CSRF 설정을 확인해 주세요.",
      404: "요청한 데이터를 찾지 못했습니다.",
      409: "이미 존재하는 데이터이거나 현재 상태에서 처리할 수 없습니다.",
      413: "업로드 파일이 너무 큽니다.",
      500: "서버 오류가 발생했습니다.",
    };

    throw new ApiError(
      response.status,
      messages[response.status] || "요청 처리에 실패했습니다.",
    );
  }

  // 다운로드 요청은 JSON 대신 파일 데이터로 반환
  if (responseType === "blob") {
    return response.blob();
  }

  return data;
}

// =====================================================
// 4. Refresh Token 갱신
// 동시에 여러 번 갱신하지 않도록 방지
// =====================================================
let refreshInFlight = null;
let sessionInFlight = null;
// 인증 복구 실패를 공유하여 각 컴포넌트의 반복 요청을 제한
let sessionRecoveryError = null;
let sessionRetryAt = 0;

function clearSessionRecoveryError() {
  sessionRecoveryError = null;
  sessionRetryAt = 0;
}

function rememberSessionRecoveryError(error) {
  sessionRecoveryError = error;
  sessionRetryAt = Date.now() + 30000;
}

function requireSessionUser(me) {
  if (me) return me;

  notifySessionExpired();

  throw new ApiError(
    401,
    "로그인 상태가 만료되었습니다. 다시 로그인해 주세요.",
  );
}

// 같은 탭의 세션 복구·토큰 갱신 요청을 순서대로 실행
let authRequestQueue = Promise.resolve();

function enqueueAuthRequest(task) {
  const result = authRequestQueue.then(task);

  // 앞 요청이 실패해도 다음 요청은 실행 가능
  authRequestQueue = result.then(
    () => undefined,
    () => undefined,
  );

  return result;
}

function refreshOnce() {
  if (!refreshInFlight) {
    refreshInFlight = enqueueAuthRequest(() =>
      request("/api/auth/refresh", {
        method: "POST",
        cache: "no-store",
      }),
    ).finally(() => {
      refreshInFlight = null;
    });
  }

  return refreshInFlight;
}

function restoreSessionOnce() {
  // 이미 확인 중이면 같은 결과를 기다림
  if (sessionInFlight) {
    return sessionInFlight;
  }

  // 실패 직후에는 새로운 인증 요청을 보내지 않음
  if (sessionRecoveryError && Date.now() < sessionRetryAt) {
    return Promise.reject(sessionRecoveryError);
  }

  sessionInFlight = enqueueAuthRequest(async () => {
    try {
      const me = await request("/api/auth/session", {
        method: "POST",
        cache: "no-store",
      });

      if (!me) {
        // 서버가 204로 비로그인 상태를 확인한 경우
        rememberSessionRecoveryError(new ApiError(401, "로그인이 필요합니다."));

        notifySessionExpired();
        return null;
      }

      clearSessionRecoveryError();
      return me;
    } catch (error) {
      rememberSessionRecoveryError(error);

      // 403이나 통신 장애만으로 로그아웃시키지 않음
      if (error instanceof ApiError && error.status === 401) {
        notifySessionExpired();
      }

      throw error;
    }
  }).finally(() => {
    sessionInFlight = null;
  });

  return sessionInFlight;
}
// =====================================================
// 5. 인증 API
// =====================================================
export const authApi = {
  // 회원가입
  signup: (email, password, nickname, signupProof, acceptedTermsIds) =>
    request("/api/auth/signup", {
      method: "POST",
      body: {
        email,
        password,
        nickname,
        signupProof,
        acceptedTermsIds,
      },
    }),

  // ====================================
  // 2. 이메일 인증번호 발송 (신규)
  // ====================================
  sendEmailCode: (email) =>
    request("/api/auth/email/send", {
      method: "POST",
      body: { email },
    }),

  // ====================================
  // 3. 이메일 인증번호 검증 (신규)
  // ====================================
  verifyEmailCode: (email, code) =>
    request("/api/auth/email/verify", {
      method: "POST",
      body: { email, code },
    }),

  checkNickname: (nickname) =>
    request(
      `/api/auth/nickname/check?nickname=${encodeURIComponent(nickname)}`,
    ),

  // 로그인
  login: (email, password) =>
    enqueueAuthRequest(async () => {
      const me = await request("/api/auth/login", {
        method: "POST",
        body: {
          email,
          password,
        },
      });

      clearSessionRecoveryError();
      return me;
    }),

  // 로그인 사용자 조회
  me: () => request("/api/auth/me"),

  // 토큰 갱신
  refresh: () => refreshOnce(),

  // 로그아웃
  logout: () =>
    enqueueAuthRequest(async () => {
      const result = await request("/api/auth/logout", {
        method: "POST",
      });

      // 화면이 정리되는 동안 남아 있는 자동 조회도 제한
      rememberSessionRecoveryError(new ApiError(401, "로그아웃되었습니다."));

      return result;
    }),

  // 새로고침 시 로그인 상태 복원
  // 사이트 접속·새로고침 시 로그인 상태 복원
  restore: () => restoreSessionOnce(),
};

// =====================================================
// 6. 공통 API 요청 (자동 Refresh 선택 지원)
// =====================================================
export async function apiRequest(path, options = {}) {
  const { retryOn401 = false, ...requestOptions } = options;

  function checkAborted() {
    if (requestOptions.signal?.aborted) {
      throw new DOMException("요청이 취소되었습니다.", "AbortError");
    }
  }

  checkAborted();

  if (retryOn401) {
    // 다른 요청이 인증을 복구하고 있다면 먼저 기다림.
    // 이전 복구가 실패했다면 제한 시간을 적용하여 다시 확인함.
    if (sessionInFlight || sessionRecoveryError) {
      const me = await restoreSessionOnce();
      checkAborted();
      requireSessionUser(me);
    }
  }

  try {
    return await request(path, requestOptions);
  } catch (error) {
    if (!retryOn401 || !(error instanceof ApiError) || error.status !== 401) {
      throw error;
    }
  }

  checkAborted();

  // 401 발생 시 /session에서 현재 사용자 확인 및 토큰 복구.
  // 동시에 실패한 요청들은 같은 복구 요청을 공유함.
  const me = await restoreSessionOnce();

  checkAborted();
  requireSessionUser(me);

  // 복구 성공 후 원래 요청은 한 번만 재시도
  try {
    return await request(path, requestOptions);
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      // 복구 직후에도 인증이 거절되면 반복 요청을 제한.
      // 서버 간 인증 설정 차이 등도 가능하므로
      // 이 결과만으로 사용자 로그아웃을 확정하지 않음.
      rememberSessionRecoveryError(error);
    }

    throw error;
  }
}

function notifySessionExpired() {
  window.dispatchEvent(new Event("spoilock:session-expired"));
}
