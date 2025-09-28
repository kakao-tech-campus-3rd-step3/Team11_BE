package com.pnu.momeet.domain.evaluation.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record EvaluationSubmittedEvent(
    UUID meetupId,
    UUID evaluatorProfileId,
    UUID targetProfileId,
    String rating,
    LocalDateTime occurredAt
) {
}
