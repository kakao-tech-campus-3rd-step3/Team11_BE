package com.pnu.momeet.domain.meetup.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MeetupFinishedEvent(
    UUID meetupId,
    List<UUID> participantProfileIds,
    LocalDateTime occurredAt
) {
}
