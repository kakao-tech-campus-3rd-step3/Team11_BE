package com.pnu.momeet.domain.badge.service.mapper;

import com.pnu.momeet.domain.badge.dto.response.BadgeAwardResponse;
import com.pnu.momeet.domain.badge.dto.response.ProfileBadgeResponse;
import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.entity.ProfileBadge;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProfileBadgeDtoMapper {

    public static ProfileBadgeResponse toProfileBadgeResponse(ProfileBadge profileBadge, Badge badge) {
        return new ProfileBadgeResponse(
            badge.getId(),
            badge.getName(),
            badge.getDescription(),
            badge.getIconUrl(),
            badge.getCode(),
            profileBadge.getCreatedAt(),
            profileBadge.getUpdatedAt(),
            profileBadge.isRepresentative()
        );
    }

    public static BadgeAwardResponse toBadgeAwardResponse(ProfileBadge profileBadge, Badge badge) {
        return new BadgeAwardResponse(
            profileBadge.getProfileId(),
            badge.getId(),
            badge.getName(),
            badge.getDescription(),
            badge.getIconUrl(),
            badge.getCode(),
            profileBadge.getCreatedAt()
        );
    }
}
