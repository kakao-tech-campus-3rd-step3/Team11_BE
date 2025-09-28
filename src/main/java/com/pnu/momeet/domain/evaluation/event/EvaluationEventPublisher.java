package com.pnu.momeet.domain.evaluation.event;

import com.pnu.momeet.common.event.CoreEventPublisher;
import com.pnu.momeet.common.event.DomainEvent;
import com.pnu.momeet.common.logging.Source;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EvaluationEventPublisher {

    private final CoreEventPublisher core;

    public void publishSubmitted(
        UUID meetupId,
        UUID evaluatorProfileId,
        UUID targetProfileId,
        Rating rating,
        Source source
    ) {
        UUID eventId = UUID.randomUUID();
        DomainEvent event = EvaluationSubmittedEvent.of(
            meetupId,
            evaluatorProfileId,
            targetProfileId,
            rating,
            LocalDateTime.now(),
            eventId
        );

        core.publish(event, source, Map.of(
            "eventId", eventId,
            "meetupId", meetupId,
            "evaluatorProfileId", evaluatorProfileId,
            "targetProfileId", targetProfileId,
            "rating", rating
        ));
    }
}
