package com.pnu.momeet.domain.meetup.enums;

public enum MeetupCategory {
    SPORTS("스포츠"),
    CAFE("카페"),
    BOARDGAME("보드게임"),
    STUDY("스터디"),
    TRAVEL("여행"),
    CULTURE("문화"),
    FOOD("음식"),
    HOBBY("취미"),
    VOLUNTEER("봉사"),
    NETWORKING("네트워킹"),
    OTHER("기타");

    private final String description;

    MeetupCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
