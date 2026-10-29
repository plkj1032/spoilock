package com.spoilock.backend.drama.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum DramaGenre {

    ROMANCE("로맨스"),
    ROMANTIC_COMEDY("로맨틱 코미디"),
    COMEDY("코미디"),
    THRILLER("스릴러"),
    MYSTERY("미스터리"),
    CRIME("범죄"),
    ACTION("액션"),
    FANTASY("판타지"),
    SCI_FI("SF"),
    HISTORICAL("사극"),
    FAMILY("가족"),
    HUMAN("휴먼"),
    MEDICAL("의학"),
    LEGAL("법정"),
    SCHOOL("학원"),
    HORROR("공포"),
    OTHER("기타");

    private final String label;
}