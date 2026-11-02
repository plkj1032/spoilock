package com.spoilock.backend.post.entity;

import com.spoilock.backend.common.entity.CreatedAtEntity;
import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.common.enums.ModerationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_media")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostMedia extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "storage_key", nullable = false, length = 80)
    private String storageKey;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "media_type", nullable = false, length = 30)
    private String mediaType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    // 관리자가 숨김(HIDDEN)·삭제(REMOVED)하지 않는 한 공개.
    // 스포일러 여부는 아래 자동 판정 결과로 블러 처리한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false)
    private ModerationStatus moderationStatus =
            ModerationStatus.VISIBLE;

    // 이 회차 이상 본 사람에게는 원본, 나머지는 블러 (null = 모두에게 원본)
    @Column(name = "required_episode_number")
    private Integer requiredEpisodeNumber;

    // 이미지 스포일러 자동 판정 상태 (COMPLETED 전에는 작성자 외에는 블러)
    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false, length = 20)
    private AnalysisStatus analysisStatus = AnalysisStatus.PENDING;

    @Column(name = "analysis_attempts", nullable = false)
    private int analysisAttempts;

    @Column(name = "analysis_updated_at")
    private LocalDateTime analysisUpdatedAt;

    // 판정 메모 (관리자·로그용, 사용자 응답에 넣지 않음)
    @Column(name = "analysis_note")
    private String analysisNote;

    public static PostMedia create(
            Post post,
            String storageKey,
            String filename,
            String mediaType,
            long sizeBytes,
            int sortOrder
    ) {
        PostMedia media = new PostMedia();

        media.post = post;
        media.storageKey = storageKey;
        media.originalFilename = filename;
        media.mediaType = mediaType;
        media.sizeBytes = sizeBytes;
        media.sortOrder = sortOrder;

        return media;
    }

    /** 관리자 검수: 자동 판정 결과를 덮어쓴다 */
    public void review(boolean approved, Integer requiredEpisode) {
        this.moderationStatus = approved
                ? ModerationStatus.VISIBLE
                : ModerationStatus.REMOVED;

        this.requiredEpisodeNumber =
                approved ? requiredEpisode : null;

        this.analysisStatus = AnalysisStatus.COMPLETED;
        this.analysisUpdatedAt = LocalDateTime.now();
        this.analysisNote = approved ? "관리자 검수" : "관리자 삭제";
    }

    /** 자동 판정 완료. requiredEpisode == null 이면 모두에게 원본 */
    public void completeAnalysis(Integer requiredEpisode, String note) {
        this.requiredEpisodeNumber =
                requiredEpisode != null && requiredEpisode > 0 ? requiredEpisode : null;
        this.analysisStatus = AnalysisStatus.COMPLETED;
        this.analysisUpdatedAt = LocalDateTime.now();
        this.analysisNote = truncate(note);
    }

    /** 자동 판정 실패 → 블러 유지, 복구 작업이 재시도 */
    public void failAnalysis(String note) {
        this.analysisStatus = AnalysisStatus.FAILED;
        this.analysisUpdatedAt = LocalDateTime.now();
        this.analysisNote = truncate(note);
    }

    private static String truncate(String value) {
        if (value == null) return null;
        return value.length() <= 255 ? value : value.substring(0, 255);
    }
}