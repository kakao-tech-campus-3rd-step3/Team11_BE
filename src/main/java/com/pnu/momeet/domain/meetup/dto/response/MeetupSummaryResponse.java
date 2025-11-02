package com.pnu.momeet.domain.meetup.dto.response;

import com.pnu.momeet.domain.meetup.enums.MainCategory;

import java.time.LocalDateTime;
import java.util.UUID;

public record MeetupSummaryResponse(
    UUID meetupId,
    String name,
    MainCategory category,
    LocalDateTime startAt,
    LocalDateTime endAt,
    int participantCount,
    int capacity,
    boolean evaluated
) {
}
