package com.pnu.momeet.domain.profile.dto.request;

import jakarta.validation.constraints.Size;

public record LocationInput(
    // Sigungu PK 매칭
    Long baseLocationId,

    // 이름 매칭
    @Size(max=20) String sidoName,
    @Size(max=20) String sigunguName,

    // 좌표 매칭
    Double latitude,
    Double longitude
) {
}
