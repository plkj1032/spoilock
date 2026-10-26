package com.spoilock.backend.report.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record ChatReportDecisionRequest(
        @NotNull
        Decision decision,

        @NotEmpty
        List<@NotNull @Positive Long>reportIds
) {
    public enum Decision {
        KICK,
        REJECT
    }
}
