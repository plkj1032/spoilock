package com.spoilock.backend.user.event;

public record LoginSessionChangedEvent(
        Long userId,
        Long tokenVersion
) {
}