package com.spoilock.backend.ai.service;

import com.spoilock.backend.ai.client.ImageJudgeResponse;

import java.util.Collections;
import java.util.List;

/**
 * 이미지 판정(Python /admin/judge-image) → 블러 기준 회차 결정.
 * 원본은 '보기' 버튼으로 언제든 볼 수 있으므로, 애매하면 블러(보수적) 쪽으로 정한다.
 * Spring 의존성이 없는 순수 클래스 (단위 테스트용).
 */
public final class ImageVerdictPolicy {

    private ImageVerdictPolicy() {
    }

    /**
     * @param failed          판정 실패 → 블러 유지, 재시도
     * @param requiredEpisode 이 회차 이상 본 사람에게 원본 (null = 모두에게 원본)
     */
    public record ImageDecision(boolean failed, Integer requiredEpisode, String note) {

        public static ImageDecision failed(String note) {
            return new ImageDecision(true, null, note);
        }

        public static ImageDecision showAll(String note) {
            return new ImageDecision(false, null, note);
        }

        public static ImageDecision blurUntil(int episode, String note) {
            return new ImageDecision(false, episode, note);
        }
    }

    public static ImageDecision decide(ImageJudgeResponse response, List<Integer> indexedEpisodes) {

        if (response == null || response.verdict() == null) {
            return ImageDecision.failed("빈 판정");
        }

        Integer last = indexedEpisodes == null || indexedEpisodes.isEmpty()
                ? null : Collections.max(indexedEpisodes);

        return switch (response.verdict()) {

            case "MATCH" -> {
                Integer ep = response.requiredEpisodeNumber();
                if (ep != null && ep > 0) {
                    yield ImageDecision.blurUntil(ep, "MATCH → " + ep + "화");
                }
                // 회차를 알 수 없는 MATCH → 마지막 회차 기준으로 블러
                yield last == null
                        ? ImageDecision.failed("MATCH 인데 회차 없음")
                        : ImageDecision.blurUntil(last, "MATCH 회차 없음 → 마지막 " + last + "화 기준");
            }

            // 스포일러 장면 같지만 어느 사건인지 모름 → 마지막 회차 기준으로 블러
            case "UNCERTAIN" -> last == null
                    ? ImageDecision.failed("UNCERTAIN 인데 판정 범위 없음")
                    : ImageDecision.blurUntil(last, "UNCERTAIN → 마지막 " + last + "화 기준");

            case "NO_MATCH" -> ImageDecision.showAll("NO_MATCH");

            // 영상 + 자막으로 분석한 사건이 없는 드라마 → 판정 대상 아님
            case "SKIPPED_NO_VIDEO_EVENTS" -> ImageDecision.showAll("영상 분석 사건 없음");

            // 드라마 장면이 아닌 이미지 (음식·셀카·밈 등)
            case "SKIPPED_NOT_SCENE" -> ImageDecision.showAll("드라마 장면 아님");

            case "ERROR" -> ImageDecision.failed("분석 서버 오류: " + nullToEmpty(response.error()));

            default -> ImageDecision.failed("알 수 없는 판정: " + response.verdict());
        };
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
