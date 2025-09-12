package com.pnu.momeet.domain.meetup.dto.request;

import com.pnu.momeet.common.validation.annotation.ValidMainCategory;
import com.pnu.momeet.common.validation.annotation.ValidSubCategory;
import com.pnu.momeet.domain.meetup.enums.MainCategory;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record MeetupCreateRequest(
        @NotBlank(message = "모임명은 필수입니다")
        @Size(min = 1, max = 60, message = "모임명은 1-60자 사이여야 합니다")
        String name,

        @NotNull(message = "카테고리는 필수입니다")
        @ValidMainCategory
        MainCategory category,

        @ValidSubCategory
        String subCategory,

        String description,

        String[] hashTags,

        @Min(value = 2, message = "최소 인원은 2명입니다")
        @Max(value = 100, message = "최대 인원은 100명입니다")
        Integer capacity,

        @Min(value = 0, message = "점수 제한은 0 이상이어야 합니다")
        @Max(value = 100, message = "점수 제한은 100 이하여야 합니다")
        Integer scoreLimit,

        @NotNull(message = "시작 시간은 필수입니다")
        LocalDateTime startAt,

        @NotNull(message = "종료 시간은 필수입니다")
        LocalDateTime endAt,

        @NotNull(message = "위치 정보는 필수입니다")
        LocationRequest location
) {
}
