package com.pnu.momeet.domain.meetup.enums;

import lombok.Getter;

@Getter
public enum MeetupStatus {
    OPEN("모집중"),
    IN_PROGRESS("진행중"),
    CANCELED("취소됨"),
    ENDED("종료"),
    EVALUATION_TIMEOUT("평가기간종료");
    private final String description;

    MeetupStatus(String description) {
        this.description = description;
    }
}
