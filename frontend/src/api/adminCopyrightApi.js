import { apiRequest } from "./authApi";

const BASE_PATH = "/api/admin/copyright/licenses";

async function copyrightRequest(path, options = {}) {
  try {
    return await apiRequest(path, {
      ...options,
      retryOn401: true,
    });
  } catch (error) {
    // 공통 API의 일반적인 409 안내를
    // 저작권 수정 충돌에 맞는 문구로 변경
    if (error.status === 409) {
      error.message =
        "다른 관리자가 먼저 수정했습니다. 최신 내용을 다시 조회한 후 수정해 주세요.";
    }

    throw error;
  }
}

export const adminCopyrightApi = {
  getHistories(licenseId, { page = 0, size = 5, signal } = {}) {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
    });

    return copyrightRequest(`${BASE_PATH}/${licenseId}/histories?${params}`, {
      cache: "no-store",
      signal,
    });
  },
  // 목록 조회
  getLicenses({
    dramaId = null,
    reviewStatus = "",
    keyword = "",
    page = 0,
    size = 20,
    signal,
  } = {}) {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
    });

    if (dramaId != null && dramaId !== "") {
      params.set("dramaId", String(dramaId));
    }

    if (reviewStatus) {
      params.set("reviewStatus", reviewStatus);
    }

    const search = keyword.trim();

    if (search) {
      params.set("keyword", search);
    }

    return copyrightRequest(`${BASE_PATH}?${params}`, {
      signal,
      cache: "no-store",
    });
  },

  // 상세 조회
  getLicense(licenseId, { signal } = {}) {
    return copyrightRequest(`${BASE_PATH}/${licenseId}`, {
      signal,
      cache: "no-store",
    });
  },

  // 등록
  createLicense(data) {
    return copyrightRequest(BASE_PATH, {
      method: "POST",
      body: {
        dramaId: Number(data.dramaId),
        ...toLicenseFields(data),
      },
    });
  },

  // 수정
  updateLicense(licenseId, data) {
    if (!Number.isSafeInteger(data.version) || data.version < 0) {
      throw new Error(
        "수정 버전을 확인할 수 없습니다. 최신 내용을 다시 조회해 주세요.",
      );
    }

    return copyrightRequest(`${BASE_PATH}/${licenseId}`, {
      method: "PUT",
      body: {
        version: data.version,
        ...toLicenseFields(data),
      },
    });
  },
};

// 입력 폼 → 서버 요청 형식
function toLicenseFields(data) {
  return {
    licenseName: (data.licenseName ?? "").trim(),
    rightsHolder: (data.rightsHolder ?? "").trim(),

    contactName: optionalText(data.contactName),
    contactEmail: optionalText(data.contactEmail),

    reviewStatus: data.reviewStatus,
    videoPermission: data.videoPermission,
    subtitlePermission: data.subtitlePermission,
    aiPermission: data.aiPermission,

    startsOn: data.startsOn || null,
    endsOn: data.endsOn || null,

    scopeNotes: optionalText(data.scopeNotes),
    adminNotes: optionalText(data.adminNotes),
  };
}

function optionalText(value) {
  const text = (value ?? "").trim();

  return text || null;
}
