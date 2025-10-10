package com.pnu.momeet.domain.profile.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TemperatureCalculator {

    private static final BigDecimal MIN  = new BigDecimal("33.0");
    private static final BigDecimal MAX  = new BigDecimal("40.0");
    private static final BigDecimal BASE = new BigDecimal("36.5");

    // 베이지안 스무딩 로직 적용 p = (L + k) / (L + D + 2k), temp = 33.0 + 7.0 * p
    public static BigDecimal bayesian(long likes, long dislikes, double priorK) {
        if (likes <= 0 && dislikes <= 0) return BASE;

        // 정밀도 보장을 위해 BigDecimal로 변환
        BigDecimal L = BigDecimal.valueOf(Math.max(0, likes));
        BigDecimal D = BigDecimal.valueOf(Math.max(0, dislikes));
        BigDecimal k = BigDecimal.valueOf(priorK);

        // 분모 = L + D + 2k (항상 양수)
        BigDecimal denominator = L.add(D).add(k.multiply(BigDecimal.valueOf(2)));

        // 스무딩된 비율 p = (L + k) / (L + D + 2k)
        // 중간 계산은 오차 누적 방지를 위해 스케일 10자리 정도로 보수적 계산
        BigDecimal p = L.add(k).divide(denominator, 10, RoundingMode.HALF_UP);
        // temp = 33.0 + 7.0 * p
        BigDecimal temp = new BigDecimal("33.0")
            .add(new BigDecimal("7.0").multiply(p));

        // NUMERIC(4,1)에 맞추어 소수 1자리 확정 + 반올림(HALF_UP)
        temp = temp.setScale(1, RoundingMode.HALF_UP);
        // [33.0, 40.0] 범위로 클램프
        if (temp.compareTo(MIN) < 0) return MIN;
        if (temp.compareTo(MAX) > 0) return MAX;
        return temp;
    }
}
