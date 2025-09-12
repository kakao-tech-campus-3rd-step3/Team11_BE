package com.pnu.momeet.domain.meetup.dto.request;

import jakarta.validation.constraints.NotNull;

public record LocationRequest(
        @NotNull(message = "위도는 필수입니다")
        Double latitude,

        @NotNull(message = "경도는 필수입니다")
        Double longitude,

        String address
) {
}
