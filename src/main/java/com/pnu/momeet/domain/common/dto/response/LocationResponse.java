package com.pnu.momeet.domain.common.dto.response;

public record LocationResponse(
    Double longitude,
    Double latitude,
    String address
) {
}
