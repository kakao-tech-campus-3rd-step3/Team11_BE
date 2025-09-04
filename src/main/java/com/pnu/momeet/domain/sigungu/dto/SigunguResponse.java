package com.pnu.momeet.domain.sigungu.dto;

import java.time.LocalDateTime;

public record SigunguResponse (
    Long id,
    String sidoName,
    String sidoCode,
    String sigunguName,
    String sigunguCode,
    BaseLocationResponse baseLocation,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}
