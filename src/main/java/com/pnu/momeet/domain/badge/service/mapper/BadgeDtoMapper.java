package com.pnu.momeet.domain.badge.service.mapper;

import com.pnu.momeet.domain.badge.dto.request.BadgePageRequest;
import com.pnu.momeet.domain.badge.dto.response.BadgeCreateResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeUpdateResponse;
import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.common.mapper.PageMapper;
import org.springframework.data.domain.PageRequest;

public final class BadgeDtoMapper {
    private BadgeDtoMapper() {}

    public static PageRequest toPageRequest(BadgePageRequest request) {
        return PageRequest.of(
            request.getPage(),
            request.getSize(),
            PageMapper.toSort(request.getSort())
        );
    }

    public static BadgeCreateResponse toCreateResponseDto(Badge badge) {
        return new BadgeCreateResponse(
            badge.getId(),
            badge.getName(),
            badge.getDescription(),
            badge.getIconUrl(),
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
}