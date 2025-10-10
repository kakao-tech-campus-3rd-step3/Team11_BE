package com.pnu.momeet.domain.meetup.dto.response;

import com.pnu.momeet.domain.meetup.enums.MainCategory;
import com.pnu.momeet.domain.meetup.enums.SubCategory;
import java.time.LocalDateTime;
import java.util.UUID;

public record MeetupSummaryResponse(
    UUID meetupId,
    String name,
    MainCategory category,
    SubCategory subCategory,
    LocalDateTime endAt,
    int participantCount,
    int capacity,
    boolean evaluated
) {
}
