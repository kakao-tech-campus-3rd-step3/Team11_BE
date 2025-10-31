package com.pnu.momeet.domain.badge.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProfileBadgeRepresentativeRequest(
    @NotNull(message = "배지 ID는 필수입니다.")
    UUID badgeId
) {
}
