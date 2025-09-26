package com.pnu.momeet.domain.badge.service.mapper;

import com.pnu.momeet.domain.badge.dto.request.BadgePageRequest;
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
}