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

  // 등록된 MP4 로 사건 분석 요청 (음성 인식 → 자막 → 사건 추출)
  // replace=true: 이미 사건이 있는 회차 재분석 (검수·수동 사건은 보존)
  requestAnalysis(episodeId, replace = false) {
    return apiRequest(
      `/api/admin/episodes/${episodeId}/analysis/video-request?replace=${replace}`,
      {
        method: "POST",
        retryOn401: true,
      },
    );
  },

  // MP4 삭제
  deleteVideo(episodeId) {
    return apiRequest(`/api/admin/episodes/${episodeId}/video`, {
      method: "DELETE",
      retryOn401: true,
    });
  },
};
