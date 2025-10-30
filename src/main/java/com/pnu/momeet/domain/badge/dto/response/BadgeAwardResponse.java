package com.pnu.momeet.domain.badge.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record BadgeAwardResponse(
    UUID targetProfileId,
    UUID badgeId,
    String badgeName,
    String badgeDescription,
    String badgeIconUrl,
    String badgeCode,
    LocalDateTime profileBadgeCreatedAt
) {
}
