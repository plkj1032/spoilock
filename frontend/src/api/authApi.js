// 같은 localhost를 쓰는 개발 환경에서는 Vite가 /api 요청을 Spring Boot(8080)로 전달합니다.
// JWT access_token / refresh_token은 HttpOnly 쿠키이며 JS에서 직접 꺼내지 않습니다.

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

function readCookie(name) {
  const prefix = `${name}=`;
  const cookie = document.cookie
    .split(";")
    .map((item) => item.trim())
    .find((item) => item.startsWith(prefix));

  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : null;
}

async function csrfToken() {
  // Spring Security SPA 설정에서 XSRF-TOKEN 쿠키를 발급/초기화합니다.
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

async function request(path, { method = "GET", body } = {}) {
  const headers = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";

  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    // SPA에서는 응답 JSON의 마스킹된 토큰이 아니라 쿠키의 원본 토큰을 사용합니다.
    headers["X-XSRF-TOKEN"] = await csrfToken();
  }

  const response = await fetch(path, {
    method,
    headers,
    credentials: "include",
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  });

  const contentType = response.headers.get("content-type") || "";
  let data = null;
  if (response.status !== 204 && contentType.includes("application/json")) {
    data = await response.json().catch(() => null);
  }

  if (!response.ok) {
    const messages = {
      400: "입력한 정보를 확인해 주세요.",
      401: "로그인이 필요하거나 인증 정보가 만료되었습니다.",
      403: "요청이 거부되었습니다. CSRF 설정을 확인해 주세요.",
      409: "이미 가입된 이메일입니다.",
      500: "서버 오류가 발생했습니다.",
    };
    throw new ApiError(
      response.status,
      messages[response.status] || "요청 처리에 실패했습니다.",
    );
  }

  return data;
}

let refreshInFlight = null;
function refreshOnce() {
  // 동시에 들어온 만료 요청이 같은 Refresh Token을 중복 회전시키지 않도록 보호
  if (!refreshInFlight) {
    refreshInFlight = request("/api/auth/refresh", { method: "POST" }).finally(
      () => {
        refreshInFlight = null;
      },
    );
  }
  return refreshInFlight;
}

export const authApi = {
  signup: (email, password, nickname) =>
    request("/api/auth/signup", {
      method: "POST",
      body: { email, password, nickname },
    }),
  login: (email, password) =>
    request("/api/auth/login", { method: "POST", body: { email, password } }),
  me: () => request("/api/auth/me"),
  refresh: () => refreshOnce(),
  logout: () => request("/api/auth/logout", { method: "POST" }),
  async restore() {
    try {
      return await this.me();
    } catch (error) {
      if (!(error instanceof ApiError) || error.status !== 401) throw error;
      try {
        await refreshOnce();
        return await this.me();
      } catch (refreshError) {
        if (refreshError instanceof ApiError && refreshError.status === 401)
          return null;
        throw refreshError;
      }
    }
  },
};
