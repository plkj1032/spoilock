package com.spoilock.backend.ai.service;

import java.util.List;

/**
 * 게시글 이미지 저장(커밋) 후 스포일러 판정을 요청하는 이벤트.
 */
public record MediaAnalysisRequested(List<Long> mediaIds) {
}
