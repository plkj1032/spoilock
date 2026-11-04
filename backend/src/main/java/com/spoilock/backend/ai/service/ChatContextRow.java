package com.spoilock.backend.ai.service;

import java.time.LocalDateTime;

/**
 * 스포 판정 맥락용 채팅 1줄 (JPQL 생성자 조회 결과)
 */
public record ChatContextRow(
        Long messageId,
        Long userId,
        String content,
        LocalDateTime createdAt
) {
}