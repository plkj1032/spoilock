package com.spoilock.backend.drama.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spoilock.backend.drama.domain.enums.DramaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static com.spoilock.backend.drama.domain.entity.QDrama.drama;
import static com.spoilock.backend.drama.domain.entity.QEpisode.episode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DramaAdminAccessService {
    private final JPAQueryFactory queryFactory;

    public void requireManageableDrama(Long dramaId) {
        if (dramaId == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "사건에 연결된 작품 정보를 확인할 수 없습니다."
            );
        }

        DramaStatus status = queryFactory
                .select(drama.status)
                .from(drama)
                .where(drama.id.eq(dramaId))
                .fetchOne();

        if (status == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "작품을 찾을 수 없습니다."
            );
        }

        if (status == DramaStatus.DELETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "삭제된 작품의 분석 사건은 검수·수정·삭제할 수 없습니다."
            );
        }
    }

    public void requireManageableEpisode(Long episodeId) {

        DramaStatus status = queryFactory
                .select(drama.status)
                .from(episode)
                .join(episode.drama,drama)
                .where(episode.id.eq(episodeId))
                .fetchOne();

        if(status == null){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "회차를 찾을 수 없습니다."
            );
        }
        if (status == DramaStatus.DELETED){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "삭제된 작품의 영상/자막/분석 정보는 관리할 수 없습니다."
            );
        }
    }
}
