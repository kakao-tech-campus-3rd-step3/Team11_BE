package com.pnu.momeet.domain.sigungu.dto;

import java.time.LocalDateTime;

public record SigunguResponse (
    String sidoName,
    Long sidoCode,
    String sigunguName,
    Long sigunguCode,
    BaseLocationResponse baseLocation,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}
