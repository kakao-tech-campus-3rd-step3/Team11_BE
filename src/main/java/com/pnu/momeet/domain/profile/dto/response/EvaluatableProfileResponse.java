package com.pnu.momeet.domain.profile.dto.response;

import com.pnu.momeet.domain.evaluation.enums.Rating;
import java.math.BigDecimal;
import java.util.UUID;

public record EvaluatableProfileResponse(
    UUID profileId,
    String nickname,
    String imageUrl,
    BigDecimal temperature,
    Rating currentEvaluation
) {
}
