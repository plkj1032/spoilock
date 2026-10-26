import { apiRequest } from "./authApi";

export const adminVideoApi = {
  // 등록된 MP4 정보 조회
  getVideo(episodeId) {
    return apiRequest(`/api/admin/episodes/${episodeId}/video`, {
      retryOn401: true,
    });
  },

  // MP4 업로드
  upload(episodeId, file) {
    const formData = new FormData();
    formData.append("file", file);

    return apiRequest(`/api/admin/episodes/${episodeId}/video`, {
      method: "POST",
      formData,
      retryOn401: true,
    });
  },

  // MP4 삭제
  deleteVideo(episodeId) {
    return apiRequest(`/api/admin/episodes/${episodeId}/video`, {
      method: "DELETE",
      retryOn401: true,
    });
  },
};
