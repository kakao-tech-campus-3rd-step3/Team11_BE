package com.pnu.momeet.domain.meetup.dto.request;

import com.pnu.momeet.domain.meetup.enums.MainCategory;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record MeetupUpdateRequest(
    @Size(min = 1, max = 60, message = "모임명은 1-60자 사이여야 합니다")
    String name,

    MainCategory category,

    String description,

    String[] tags,

    String[] hashTags,

    @Min(value = 2, message = "최소 인원은 2명입니다")
    @Max(value = 100, message = "최대 인원은 100명입니다")
    Integer capacity,

    @Min(value = 0, message = "점수 제한은 0 이상이어야 합니다")
    @Max(value = 100, message = "점수 제한은 100 이하여야 합니다")
    Integer scoreLimit,

    LocalDateTime startAt,

    LocalDateTime endAt,

    LocationRequest location
) {

}