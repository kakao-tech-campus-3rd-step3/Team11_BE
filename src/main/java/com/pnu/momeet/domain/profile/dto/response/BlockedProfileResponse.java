package com.pnu.momeet.domain.profile.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BlockedProfileResponse(
    UUID profileId,
    String nickname,
    String imageUrl,
    BigDecimal temperature,
    LocalDateTime blockedAt
) {
}
