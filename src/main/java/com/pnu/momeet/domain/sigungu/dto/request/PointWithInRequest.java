package com.pnu.momeet.domain.sigungu.dto.request;

import jakarta.validation.constraints.NotNull;

public record PointWithInRequest(
        @NotNull(message = "위도는 필수입니다.")
        Double latitude,
        @NotNull(message = "경도는 필수입니다.")
        Double longitude
) {
}
