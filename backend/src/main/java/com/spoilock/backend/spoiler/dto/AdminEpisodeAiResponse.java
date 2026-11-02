package com.spoilock.backend.spoiler.dto;

import com.spoilock.backend.spoiler.entity.EpisodeAnalysis;

import java.time.LocalDateTime;

public record AdminEpisodeAiResponse (
        Long episodeId,
        String subtitleFilename,
        String analysisStatus,
        Integer eventCount,
        String errorMessage,
        LocalDateTime requestedAt,
        LocalDateTime analyzedAt,
        // 분석 진행률 0 ~ 100 (진행 전이면 null)
        Integer progress,
        // SUBTITLE / VIDEO (분석 요청 전이면 null)
        String analysisSource,
        // 영상을 분석 서버로 보내는 중 (분석 작업 생성 전)
        boolean uploading
){
    public static AdminEpisodeAiResponse empty(Long episodeId)
    {
        return new AdminEpisodeAiResponse(
                episodeId, null, "NOT_UPLOADED",
                0, null, null, null, null, null, false
        );
    }
    public static AdminEpisodeAiResponse from(
            EpisodeAnalysis analysis
    )
    {
        return new AdminEpisodeAiResponse(
                analysis.getEpisode().getId(),
                analysis.getSubtitleFilename(),
                analysis.getAnalysisStatus().name(),
                analysis.getEventCount(),
                analysis.getErrorMessage(),
                analysis.getAnalysisRequestedAt(),
                analysis.getAnalyzedAt(),
                analysis.getAnalysisProgress(),
                analysis.getAnalysisSource() == null
                        ? null : analysis.getAnalysisSource().name(),
                analysis.isUploading()
        );
    }
}
