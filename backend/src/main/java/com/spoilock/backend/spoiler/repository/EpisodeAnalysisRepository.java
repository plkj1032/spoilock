package com.spoilock.backend.spoiler.repository;

import com.spoilock.backend.common.enums.AnalysisStatus;
import com.spoilock.backend.spoiler.entity.EpisodeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EpisodeAnalysisRepository
    extends JpaRepository<EpisodeAnalysis, Long> {

    Optional<EpisodeAnalysis> findByEpisode_Id(Long episodeId);

    boolean existsByEpisode_Drama_Id(Long dramaId);

    // 대기·진행 중인 Python 분석 작업 (주기적 상태 조회 대상)
    @Query("""
            select new com.spoilock.backend.spoiler.repository.ActiveAnalysisJob(
                a.episode.id, a.analysisJobId, a.analysisSource)
            from EpisodeAnalysis a
            where a.analysisJobId is not null
              and a.analysisRequestedAt is not null
              and a.analysisStatus in :statuses
            """)
    List<ActiveAnalysisJob> findActiveJobs(
            @Param("statuses") Collection<AnalysisStatus> statuses
    );

    // 회차의 작품 ID·회차 번호 (트랜잭션 밖에서도 지연 로딩 없이 사용)
    @Query("""
            select new com.spoilock.backend.spoiler.repository.EpisodeKbRef(
                e.id, e.drama.id, e.episodeNumber)
            from Episode e
            where e.id = :episodeId
            """)
    Optional<EpisodeKbRef> findKbRef(@Param("episodeId") Long episodeId);

}
