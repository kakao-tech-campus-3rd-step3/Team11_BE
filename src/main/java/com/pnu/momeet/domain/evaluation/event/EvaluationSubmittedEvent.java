package com.pnu.momeet.domain.evaluation.event;

import com.pnu.momeet.common.event.DomainEvent;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import java.time.LocalDateTime;
import java.util.UUID;

public record EvaluationSubmittedEvent(
    UUID meetupId,
    UUID evaluatorProfileId,
    UUID targetProfileId,
    Rating rating,
    LocalDateTime occurredAt,
    UUID eventId
) implements DomainEvent {

    public static EvaluationSubmittedEvent of(
        UUID meetupId,
        UUID evaluatorProfileId,
        UUID targetProfileId,
        Rating rating,
        LocalDateTime occurredAt,
        UUID eventId
    ) {
        return new EvaluationSubmittedEvent(meetupId, evaluatorProfileId, targetProfileId, rating, occurredAt, eventId);
    }
}
