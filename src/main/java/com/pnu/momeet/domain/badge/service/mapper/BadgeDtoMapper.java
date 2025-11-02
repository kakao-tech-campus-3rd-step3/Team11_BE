package com.pnu.momeet.domain.badge.service.mapper;

import com.pnu.momeet.domain.badge.dto.response.BadgeCreateResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeUpdateResponse;
import com.pnu.momeet.domain.badge.entity.Badge;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BadgeDtoMapper {

    public static BadgeCreateResponse toCreateResponseDto(Badge badge) {
        return new BadgeCreateResponse(
            badge.getId(),
            badge.getName(),
            badge.getDescription(),
            badge.getIconUrl(),
            badge.getCode(),
            badge.getCreatedAt()
        );
    }

    public static BadgeUpdateResponse toUpdateResponseDto(Badge badge) {
        return new BadgeUpdateResponse(
            badge.getId(),
            badge.getName(),
            badge.getDescription(),
            badge.getIconUrl(),
            badge.getCreatedAt(),
            badge.getUpdatedAt()
        );
    }

    public static BadgeResponse toBadgeResponse(Badge badge) {
        return new BadgeResponse(
            badge.getId(),
            badge.getName(),
            badge.getDescription(),
            badge.getIconUrl(),
            badge.getCode(),
            badge.getCreatedAt(),
            badge.getUpdatedAt()
        );
    }
}