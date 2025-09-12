package com.pnu.momeet.domain.meetup.dto.request;

import com.pnu.momeet.common.validation.annotation.ValidMainCategory;
import com.pnu.momeet.common.validation.annotation.ValidSubCategory;
import jakarta.validation.constraints.NotNull;

public record MeetupGeoSearchRequest(
        @NotNull(message = "위도는 필수입니다.")
        Double latitude,

        @NotNull(message = "경도는 필수입니다.")
        Double longitude,

        @NotNull(message = "거리 반경은 필수입니다.(단위: km)")
        Double radius,

        @ValidMainCategory
        String category,

        @ValidSubCategory
        String subCategory,

        String search
) {

}
