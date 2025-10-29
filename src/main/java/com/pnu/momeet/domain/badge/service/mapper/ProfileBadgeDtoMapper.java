package com.pnu.momeet.domain.badge.service.mapper;

import com.pnu.momeet.domain.badge.dto.response.ProfileBadgeResponse;
import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.entity.ProfileBadge;

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
}
