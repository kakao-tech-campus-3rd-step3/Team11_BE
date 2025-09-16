package com.pnu.momeet.domain.evaluation.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record EvaluationResponse(
    UUID id,
    UUID meetupId,
    UUID evaluatorProfileId,
    UUID targetProfileId,
    String rating,
    LocalDateTime createdAt
) {
}