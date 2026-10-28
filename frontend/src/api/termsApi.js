import { apiRequest } from "./authApi";

// =====================================================
// SPOILOCK 약관 관리 API
// =====================================================
//
// 일반 사용자
// GET /api/terms/current
//
// 관리자 전용
// GET  /api/admin/terms
// POST /api/admin/terms
// PUT  /api/admin/terms/{id}
// POST /api/admin/terms/{id}/publish
//
// =====================================================

export const termsApi = {
  // ===================================================
  // 1. 회원가입 화면 - 현재 게시된 약관 조회
  // ===================================================
  getCurrent: () =>
    apiRequest("/api/terms/current", {
      method: "GET",
      cache: "no-store",
    }),

  // 기존 current() 호출 코드와 호환
  current: () =>
    apiRequest("/api/terms/current", {
      method: "GET",
      cache: "no-store",
    }),

  // ===================================================
  // 2. 관리자 - 전체 약관 조회
  // DRAFT / PUBLISHED / ARCHIVED
  // ===================================================
  getAll: () =>
    apiRequest("/api/admin/terms", {
      method: "GET",
      retryOn401: true,
    }),

  // 기존 all() 호출 코드와 호환
  all: () =>
    apiRequest("/api/admin/terms", {
      method: "GET",
      retryOn401: true,
    }),

  // ===================================================
  // 3. 관리자 - 약관 초안 생성
  // ===================================================
  create: (payload) =>
    apiRequest("/api/admin/terms", {
      method: "POST",
      body: payload,
      retryOn401: true,
    }),

  // ===================================================
  // 4. 관리자 - 기존 초안 수정
  // 게시된 약관은 수정 불가능
  // ===================================================
  update: (id, payload) =>
    apiRequest(`/api/admin/terms/${id}`, {
      method: "PUT",
      body: payload,
      retryOn401: true,
    }),

  // ===================================================
  // 5. 관리자 - 약관 게시
  // DRAFT -> PUBLISHED
  // 기존 게시 약관 -> ARCHIVED
  // ===================================================
  publish: (id) =>
    apiRequest(`/api/admin/terms/${id}/publish`, {
      method: "POST",
      retryOn401: true,
    }),
};
