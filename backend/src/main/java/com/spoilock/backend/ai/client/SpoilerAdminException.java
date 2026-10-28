package com.spoilock.backend.ai.client;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Python 관리자 API 호출 실패.
 * Python 응답 코드를 관리자 화면에 맞는 코드로 바꿔 전달한다.
 * - 400/422 → 400, 404 → 404, 409 → 409, 413 → 413
 * - 401/403(Spring ↔ Python 토큰 불일치) → 502
 *   (그대로 401 을 내보내면 프론트가 로그인 만료로 보고 토큰 갱신을 시도함)
 * - 연결 실패 → 503, 그 외 → 502
 */
public class SpoilerAdminException extends ResponseStatusException {

    private final int upstreamStatus;

    public SpoilerAdminException(int upstreamStatus, String message) {
        super(map(upstreamStatus), message);
        this.upstreamStatus = upstreamStatus;
    }

    public SpoilerAdminException(int upstreamStatus, String message, Throwable cause) {
        super(map(upstreamStatus), message, cause);
        this.upstreamStatus = upstreamStatus;
    }

    /** Python 이 돌려준 원래 상태 코드 (연결 실패는 503) */
    public int upstreamStatus() {
        return upstreamStatus;
    }

    public boolean isNotFound() {
        return upstreamStatus == 404;
    }

    public boolean isConflict() {
        return upstreamStatus == 409;
    }

    private static HttpStatus map(int status) {
        return switch (status) {
            case 400, 422 -> HttpStatus.BAD_REQUEST;
            case 404 -> HttpStatus.NOT_FOUND;
            case 409 -> HttpStatus.CONFLICT;
            case 413 -> HttpStatus.CONTENT_TOO_LARGE;
            case 503 -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }
}
