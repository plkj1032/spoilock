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
async function csrfToken() {
  const response = await fetch("/api/auth/csrf", {
    method: "GET",
    credentials: "include",
    cache: "no-store",
  });

  if (!response.ok) {
    throw new ApiError(response.status, "CSRF 토큰을 가져오지 못했습니다.");
  }

  const token = readCookie("XSRF-TOKEN");

  if (!token) {
    throw new Error(
      "XSRF-TOKEN 쿠키가 없습니다. Spring Security CSRF 설정을 확인해 주세요.",
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
  { method = "GET", body, formData, signal, cache } = {},
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

  // POST, PUT, PATCH, DELETE 등의 요청에 CSRF 적용
  if (!["GET", "HEAD", "OPTIONS"].includes(requestMethod)) {
    headers["X-XSRF-TOKEN"] = await csrfToken();
  }

  // JSON 또는 FormData 요청 본문 생성
  const requestBody =
    formData !== undefined
      ? formData
      : body !== undefined
        ? JSON.stringify(body)
        : undefined;

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

  return data;
}

// =====================================================
// 4. Refresh Token 갱신
// 동시에 여러 번 갱신하지 않도록 방지
// =====================================================
let refreshInFlight = null;

function refreshOnce() {
  if (!refreshInFlight) {
    refreshInFlight = request("/api/auth/refresh", {
      method: "POST",
    }).finally(() => {
      refreshInFlight = null;
    });
  }

  return refreshInFlight;
}

// =====================================================
// 5. 인증 API
// =====================================================
export const authApi = {
  // 회원가입
  signup: (email, password, nickname) =>
    request("/api/auth/signup", {
      method: "POST",
      body: {
        email,
        password,
        nickname,
      },
    }),

  // 로그인
  login: (email, password) =>
    request("/api/auth/login", {
      method: "POST",
      body: {
        email,
        password,
      },
    }),

  // 로그인 사용자 조회
  me: () => request("/api/auth/me"),

  // 토큰 갱신
  refresh: () => refreshOnce(),

  // 로그아웃
  logout: () =>
    request("/api/auth/logout", {
      method: "POST",
    }),

  // 새로고침 시 로그인 상태 복원
  async restore() {
    try {
      return await this.me();
    } catch (error) {
      if (!(error instanceof ApiError) || error.status !== 401) {
        throw error;
      }

      try {
        await refreshOnce();
        return await this.me();
      } catch (refreshError) {
        if (refreshError instanceof ApiError && refreshError.status === 401) {
          return null;
        }

        throw refreshError;
      }
    }
  },
};

// =====================================================
// 6. 공통 API 요청 (자동 Refresh 선택 지원)
// =====================================================
export async function apiRequest(path, options = {}) {
  const { retryOn401 = false, ...requestOptions } = options;

  try {
    return await request(path, requestOptions);
  } catch (error) {
    // 자동 갱신을 사용하지 않거나 401이 아니면 예외 발생
    if (!retryOn401 || !(error instanceof ApiError) || error.status !== 401) {
      throw error;
    }

    // Access Token 만료 시 Refresh Token 갱신
    await refreshOnce();

    // 원래 요청 다시 실행
    return request(path, requestOptions);
  }
}
