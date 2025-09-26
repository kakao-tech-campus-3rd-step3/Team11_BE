package com.pnu.momeet.domain.badge.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record BadgeCreateResponse(
    UUID badgeId,
    String name,
    String description,
    String iconUrl,
    String code,
    LocalDateTime createdAt
) {
}
