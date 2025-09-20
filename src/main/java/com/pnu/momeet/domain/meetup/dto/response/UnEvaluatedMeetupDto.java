package com.pnu.momeet.domain.meetup.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record UnEvaluatedMeetupDto(
    UUID meetupId,
    String name,
    String category,
    String subCategory,
    LocalDateTime startAt,
    LocalDateTime endAt,
    int participantCount,
    int capacity,
    long unEvaluatedCount
) {
}
