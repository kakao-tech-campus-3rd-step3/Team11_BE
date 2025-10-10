package com.pnu.momeet.unit.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pnu.momeet.domain.profile.service.TemperatureCalculator;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TemperatureCalculatorTest {

    @Test
    @DisplayName("평가가 없으면 기본 온도 36.5를 반환한다")
    void zeroZero_returnsBase() {
        assertEquals(new BigDecimal("36.5"),
            TemperatureCalculator.bayesian(0, 0, 5.0));
    }

    @Test
    @DisplayName("좋아요가 많으면 온도가 상승하고 싫어요가 많으면 온도가 하락한다")
    void directionality_likeUp_dislikeDown() {
        BigDecimal up = TemperatureCalculator.bayesian(10, 0, 5.0);
        BigDecimal down = TemperatureCalculator.bayesian(0, 10, 5.0);

        assertTrue(up.compareTo(new BigDecimal("36.5")) > 0, "likes > dislikes 이면 상승");
        assertTrue(down.compareTo(new BigDecimal("36.5")) < 0, "dislikes > likes 이면 하락");
    }

    @Test
    @DisplayName("베이지안 스무딩: 표본이 클수록 기준값에서 더 멀어진다")
    void smoothing_moreSamples_moveFurther() {
        BigDecimal small = TemperatureCalculator.bayesian(1, 0, 5.0);
        BigDecimal large = TemperatureCalculator.bayesian(10, 0, 5.0);

        assertTrue(large.compareTo(small) > 0, "likes가 많을수록 더 상승");
    }

    @Test
    @DisplayName("최솟값/최댓값 클램프 및 소수 1자리 스케일을 보장한다")
    void clamped_and_oneDecimalScale() {
        BigDecimal hi = TemperatureCalculator.bayesian(1_000_000, 0, 5.0);
        BigDecimal lo = TemperatureCalculator.bayesian(0, 1_000_000, 5.0);

        assertEquals(new BigDecimal("40.0"), hi);
        assertEquals(new BigDecimal("33.0"), lo);
        assertEquals(1, hi.scale());
        assertEquals(1, lo.scale());
    }
}
