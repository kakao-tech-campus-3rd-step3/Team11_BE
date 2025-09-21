package com.pnu.momeet.domain.badge.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record BadgeResponse(
    UUID badgeId,
    String name,
    String description,
    String iconUrl,
    LocalDateTime createdAt,
    boolean representative
) {
}
