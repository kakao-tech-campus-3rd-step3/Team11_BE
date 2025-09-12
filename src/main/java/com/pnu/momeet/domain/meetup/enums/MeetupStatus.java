package com.pnu.momeet.domain.meetup.enums;

public enum MeetupStatus {
    OPEN("모집중"),
    IN_PROGRESS("진행중"),
    ENDED("종료"),
    CANCELLED("취소");

    private final String description;

    MeetupStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
