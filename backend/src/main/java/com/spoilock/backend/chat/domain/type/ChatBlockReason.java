package com.spoilock.backend.chat.domain.type;

/**
 * 메시지 원문을 보여주지 않는 이유 (visibility=BLOCK 일 때만 값이 있음).
 * 사건 내용·판단 근거는 절대 담지 않는다.
 */
public enum ChatBlockReason {
    ANALYZING,      // 스포일러 분석 대기·진행 중
    NOT_WATCHED,    // 시청 회차가 부족함
    REVIEW,         // 자동 판정으로 공개할 수 없어 검토 대기
    FAILED,         // 분석 실패 (자동 재시도 중)
    HIDDEN          // 관리자가 숨김·삭제
}
