package com.pnu.momeet.domain.meetup.event;

import com.pnu.momeet.common.event.DomainEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MeetupFinishedEvent(
    UUID meetupId,
    List<UUID> participantProfileIds,
    LocalDateTime occurredAt,
    UUID eventId
) implements DomainEvent {

    public static MeetupFinishedEvent of(
        UUID meetupId,
        List<UUID> participantProfileIds,
        LocalDateTime occurredAt,
        UUID eventId
    ) {
        return new MeetupFinishedEvent(meetupId, participantProfileIds, occurredAt, eventId);
    }
}
