package com.pnu.momeet.domain.sigungu.dto.response;

import com.pnu.momeet.domain.common.dto.response.LocationResponse;

import java.time.LocalDateTime;

public record SigunguResponse (
    String sidoName,
    Long sidoCode,
    String sigunguName,
    Long sigunguCode,
    LocationResponse baseLocation,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}
