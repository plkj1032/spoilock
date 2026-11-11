import { apiRequest } from "./authApi";

function basePath(licenseId) {
  return `/api/admin/copyright/licenses/${licenseId}/evidences`;
}

export const adminCopyrightEvidenceApi = {
  getEvidences(licenseId, { page = 0, size = 10, signal } = {}) {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
    });

    return apiRequest(`${basePath(licenseId)}?${params}`, {
      retryOn401: true,
      cache: "no-store",
      signal,
    });
  },

  upload(licenseId, file) {
    const formData = new FormData();

    // 백엔드 @RequestPart("file")과 같은 이름
    formData.append("file", file);

    return apiRequest(basePath(licenseId), {
      method: "POST",
      formData,
      retryOn401: true,
    });
  },

  download(licenseId, evidenceId, { signal } = {}) {
    return apiRequest(`${basePath(licenseId)}/${evidenceId}/download`, {
      responseType: "blob",
      retryOn401: true,
      cache: "no-store",
      signal,
    });
  },
};
