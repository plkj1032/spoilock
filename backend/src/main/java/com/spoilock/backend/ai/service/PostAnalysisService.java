
package com.spoilock.backend.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostAnalysisService {

    private final PostAnalysisStateService stateService;
    private final SpoilerJudgeService judgeService;
    private final AuthorProgressService authorProgressService;

    public void analyze(PostAnalysisRequested event) {

        // 분석할 게시글의 수정 버전 확인
        if (!stateService.tryStart(
                event.postId(),
                event.revision()
        )) {
            return;
        }

        long startedAt = System.currentTimeMillis();

        Integer authorWatched =
                authorProgressService.forPost(
                        event.postId(),
                        event.dramaId()
                );

        AnalysisDecision decision = judgeService.judge(
                event.postId(),
                event.dramaId(),
                event.content(),
                authorWatched
        );

        boolean applied;

        try {
            applied = stateService.apply(
                    event.postId(),
                    event.revision(),
                    decision
            );
        } catch (Exception e) {
            log.error(
                    "[spoiler] 분석 결과 저장 실패 postId={}",
                    event.postId(),
                    e
            );
            return;
        }

        log.info(
                "[spoiler] postId={} revision={} applied={} {}ms",
                event.postId(),
                event.revision(),
                applied,
                System.currentTimeMillis() - startedAt
        );
    }
}
