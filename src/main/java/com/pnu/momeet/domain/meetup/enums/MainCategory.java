package com.pnu.momeet.domain.meetup.enums;

import lombok.Getter;

@Getter
public enum MainCategory {
    SPORTS("스포츠"),
    FOOD("식사"),
    CULTURE_ART("문화/예술"),
    STUDY("스터디"),
    TRAVEL("여행"),
    GAME("게임"),
    OTAKU("덕질");

    private final String description;

    MainCategory(String description) {
        this.description = description;
    }
}
