package com.pnu.momeet.domain.meetup.enums;

import lombok.Getter;

@Getter
public enum SubCategory {
    // SPORTS
    SOCCER(MainCategory.SPORTS, "축구"),
    BASKETBALL(MainCategory.SPORTS, "농구"),
    BASEBALL(MainCategory.SPORTS, "야구"),
    BADMINTON(MainCategory.SPORTS, "배드민턴"),
    TENNIS(MainCategory.SPORTS, "테니스"),
    TABLE_TENNIS(MainCategory.SPORTS, "탁구"),
    BILLIARDS(MainCategory.SPORTS, "당구"),
    POOL(MainCategory.SPORTS, "포켓볼"),
    FITNESS(MainCategory.SPORTS, "헬스"),
    RUNNING(MainCategory.SPORTS, "러닝"),
    HIKING(MainCategory.SPORTS, "등산"),
    CLIMBING(MainCategory.SPORTS, "클라이밍"),

    // FOOD
    RICE(MainCategory.FOOD, "밥"),
    ALCOHOL(MainCategory.FOOD, "술"),
    CAFE(MainCategory.FOOD, "카페"),
    DESSERT(MainCategory.FOOD, "디저트"),
    LATE_NIGHT(MainCategory.FOOD, "야식"),

    // CULTURE_ART
    MOVIE(MainCategory.CULTURE_ART, "영화"),
    PLAY(MainCategory.CULTURE_ART, "연극"),
    MUSICAL(MainCategory.CULTURE_ART, "뮤지컬"),
    EXHIBITION(MainCategory.CULTURE_ART, "전시회"),
    ART_MUSEUM(MainCategory.CULTURE_ART, "미술관"),
    CONCERT(MainCategory.CULTURE_ART, "콘서트"),
    PHOTOGRAPHY(MainCategory.CULTURE_ART, "사진"),
    WRITING(MainCategory.CULTURE_ART, "글쓰기"),
    DANCE(MainCategory.CULTURE_ART, "댄스"),

    // STUDY
    READING(MainCategory.STUDY, "독서"),
    ENTRANCE_EXAM(MainCategory.STUDY, "입시"),
    PUBLIC_EXAM(MainCategory.STUDY, "공시"),
    LANGUAGE(MainCategory.STUDY, "외국어"),
    CERTIFICATE(MainCategory.STUDY, "자격증"),
    DEBATE(MainCategory.STUDY, "토론"),
    INTERVIEW(MainCategory.STUDY, "면접"),

    // TRAVEL
    DOMESTIC(MainCategory.TRAVEL, "국내"),
    ABROAD(MainCategory.TRAVEL, "해외"),
    CAMPING(MainCategory.TRAVEL, "캠핑"),
    GLAMPING(MainCategory.TRAVEL, "글램핑"),
    PICNIC(MainCategory.TRAVEL, "피크닉"),
    SEA(MainCategory.TRAVEL, "바다"),
    HIKING_TRAVEL(MainCategory.TRAVEL, "등산"),
    DRIVE(MainCategory.TRAVEL, "드라이브"),
    FISHING(MainCategory.TRAVEL, "낚시"),
    WALK(MainCategory.TRAVEL, "동네 산책"),

    // GAME
    BOARD_GAME(MainCategory.GAME, "보드게임"),
    PC_CAFE(MainCategory.GAME, "PC방"),
    MOBILE_GAME(MainCategory.GAME, "모바일게임"),
    ESCAPE_ROOM(MainCategory.GAME, "방탈출"),

    // OTAKU
    COMIC_ANIME(MainCategory.OTAKU, "만화/애니"),
    FIGURE(MainCategory.OTAKU, "피규어"),
    COSPLAY(MainCategory.OTAKU, "코스");

    private final MainCategory mainCategory;
    private final String description;

    SubCategory(MainCategory mainCategory, String description) {
        this.mainCategory = mainCategory;
        this.description = description;
    }
}

