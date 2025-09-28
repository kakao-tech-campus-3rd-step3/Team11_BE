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

    // 전체 배지 조회용
    public static final Set<String> ALL_BADGES_ALLOWED_SORTS =
        Set.of("createdAt", "name");
    public static final Sort ALL_BADGES_DEFAULT_SORT =
        Sort.by(Sort.Order.desc("createdAt"));

    // 프로필 배지 조회용
    public static final Set<String> PROFILE_BADGES_ALLOWED_SORTS =
        Set.of("representative", "createdAt", "name");
    public static final Sort PROFILE_BADGES_DEFAULT_SORT =
        Sort.by(Sort.Order.desc("representative"), Sort.Order.desc("createdAt"));

    private BadgeDtoMapper() {}

    public static PageRequest toProfileBadgePageRequest(ProfileBadgePageRequest request) {
        Sort sort = PageMapper.toSortOrDefault(
            request.getSort(),
            PROFILE_BADGES_ALLOWED_SORTS,      // 화이트리스트
            PROFILE_BADGES_DEFAULT_SORT        // 기본 정렬
        );
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }

    public static PageRequest toBadgePageRequest(BadgePageRequest request) {
        Sort sort = PageMapper.toSortOrDefault(
            request.getSort(),
            ALL_BADGES_ALLOWED_SORTS,      // 화이트리스트
            ALL_BADGES_DEFAULT_SORT        // 기본 정렬
        );
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }

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