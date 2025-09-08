package com.pnu.momeet.domain.sigungu.service.mapper;

import com.pnu.momeet.domain.common.mapper.PageMapper;
import com.pnu.momeet.domain.sigungu.dto.request.SigunguPageRequest;
import org.springframework.data.domain.PageRequest;


public class SigunguDtoMapper {

    private SigunguDtoMapper() {
        // private constructor to prevent instantiation
    }

    public static PageRequest toPageRequest(SigunguPageRequest request) {
        if (request.getSort() == null || request.getSort().isEmpty()) {
            return PageRequest.of(request.getPage(), request.getSize());
        }
        return PageRequest.of(request.getPage(), request.getSize(), PageMapper.toSort(request.getSort()));
    }

}
