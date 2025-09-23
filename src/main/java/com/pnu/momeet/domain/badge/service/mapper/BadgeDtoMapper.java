package com.pnu.momeet.domain.badge.service.mapper;

import com.pnu.momeet.domain.badge.dto.request.BadgePageRequest;
import com.pnu.momeet.domain.badge.dto.request.ProfileBadgePageRequest;
import com.pnu.momeet.domain.badge.dto.response.BadgeCreateResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeResponse;
import com.pnu.momeet.domain.badge.dto.response.ProfileBadgeResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeUpdateResponse;
import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.common.mapper.PageMapper;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class BadgeDtoMapper {
    private BadgeDtoMapper() {}

    public static PageRequest toProfileBadgePageRequest(ProfileBadgePageRequest request) {
        return PageRequest.of(
            request.getPage(),
            request.getSize(),
            PageMapper.toSort(request.getSort())
        );
    }

    public static PageRequest toBadgePageRequest(BadgePageRequest request) {
        Sort sort = PageMapper.toSortOrDefault(
            request.getSort(),
            Set.of("createdAt", "name"),                 // 화이트리스트
            Sort.by(Sort.Order.desc("createdAt"))        // 기본 정렬
        );
        return PageRequest.of(request.getPage(), request.getSize(), sort);
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

    public static BadgeResponse toBadgeResponse(Badge badge) {
        return new BadgeResponse(
            badge.getId(),
            badge.getName(),
            badge.getDescription(),
            badge.getIconUrl(),
            badge.getCreatedAt(),
            badge.getUpdatedAt()
        );
    }
}