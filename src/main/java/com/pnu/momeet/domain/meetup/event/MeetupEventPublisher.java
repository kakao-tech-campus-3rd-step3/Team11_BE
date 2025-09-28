package com.pnu.momeet.domain.meetup.event;

import com.pnu.momeet.common.event.CoreEventPublisher;
import com.pnu.momeet.common.event.DomainEvent;
import com.pnu.momeet.common.logging.Source;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetupEventPublisher {

    private final CoreEventPublisher core;

    public void publishFinished(
        UUID meetupId,
        List<UUID> participantProfileIds,
        Source source
    ) {
        UUID eventId = UUID.randomUUID();
        DomainEvent event = MeetupFinishedEvent.of(
            meetupId,
            participantProfileIds,
            LocalDateTime.now(),
            eventId
        );

        core.publish(event, source, Map.of(
            "eventId", eventId,
            "meetupId", meetupId,
            "participants", participantProfileIds == null ? 0 : participantProfileIds.size()
        ));
    }
}
