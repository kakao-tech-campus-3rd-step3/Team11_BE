package com.pnu.momeet.domain.meetup.dto.request;

import com.pnu.momeet.common.validation.annotation.ValidMainCategory;
import com.pnu.momeet.common.validation.annotation.ValidSubCategory;
import jakarta.validation.constraints.NotNull;

public record MeetupSearchRequest(
        @NotNull(message = "시군구 ID는 필수입니다.")
        Long sigunguId,

        @ValidMainCategory
        String category,

        @ValidSubCategory
        String subCategory,

        String search
) {
}
