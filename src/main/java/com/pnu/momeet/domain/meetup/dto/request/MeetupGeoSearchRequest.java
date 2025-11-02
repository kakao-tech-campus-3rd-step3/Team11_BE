package com.pnu.momeet.domain.meetup.dto.request;

import com.pnu.momeet.common.validation.annotation.ValidMainCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public record MeetupGeoSearchRequest(
        @NotNull(message = "위도는 필수입니다.")
        Double latitude,

        @NotNull(message = "경도는 필수입니다.")
        Double longitude,

        @Min(value = 1, message = "반경은 1km 이상이어야 합니다.")
        @Max(value = 100, message = "반경은 100km 이하이어야 합니다.")
        Double radius,

        @ValidMainCategory
        String category,

        String search
) {
        public MeetupGeoSearchRequest(
                Double latitude,
                Double longitude,
                Double radius,
                String category,
                String search
        ) {
                this.latitude = latitude;
                this.longitude = longitude;
                this.radius = radius != null ? radius : 5.0; // 기본 반경 5km
                this.category = category;
                this.search = search;
        }
}
