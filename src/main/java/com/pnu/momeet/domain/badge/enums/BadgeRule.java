package com.pnu.momeet.domain.badge.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BadgeRule {
    FIRST_JOIN("FIRST_JOIN", Metric.JOIN_COUNT, 1),
    TEN_JOINS("TEN_JOINS", Metric.JOIN_COUNT, 10),
    LIKE_10("LIKE_10", Metric.LIKE_COUNT, 10);

    private final String code;      // 실제 부여할 배지 코드
    private final Metric metric;    // 평가 지표
    private final int threshold;    // 평가 지표 임계치
}
