package com.pnu.momeet.domain.meetup.dto.request;

import com.pnu.momeet.common.validation.annotation.ValidMainCategory;
import com.pnu.momeet.common.validation.annotation.ValidSubCategory;
import jakarta.validation.constraints.NotNull;

public record MeetupGeoSearchRequest(
        @NotNull(message = "위도는 필수입니다.")
        Double latitude,

        @NotNull(message = "경도는 필수입니다.")
        Double longitude,

        Double radius,

        @ValidMainCategory
        String category,

        @ValidSubCategory
        String subCategory,

        String search
) {
        public MeetupGeoSearchRequest(
                Double latitude,
                Double longitude,
                Double radius,
                String category,
                String subCategory,
                String search
        ) {
                this.latitude = latitude;
                this.longitude = longitude;
                this.radius = radius != null ? radius : 5.0; // 기본 반경 5km
                this.category = category;
                this.subCategory = subCategory;
                this.search = search;
        }
}
