package com.pnu.momeet.domain.sigungu.service;

import com.pnu.momeet.domain.sigungu.dto.request.SigunguPageRequest;
import com.pnu.momeet.domain.sigungu.dto.response.SigunguResponse;
import com.pnu.momeet.domain.sigungu.service.mapper.SigunguDtoMapper;
import com.pnu.momeet.domain.sigungu.service.mapper.SigunguEntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SigunguDomainService {
    private final SigunguEntityService entityService;
    private final GeometryFactory geometryFactory;

    @Transactional(readOnly = true)
    public SigunguResponse getById(Long id) {
        return SigunguEntityMapper.toDto(
            entityService.getById(id)
        );
    }

    @Transactional(readOnly = true)
    public SigunguResponse getByPointIn(Point point) {
        return SigunguEntityMapper.toDto(
            entityService.getByPointIn(point)
        );
    }

    @Transactional(readOnly = true)
    public SigunguResponse getByPointIn(double latitude, double longitude) {
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        return getByPointIn(point);
    }

    @Transactional(readOnly = true)
    public Page<SigunguResponse> findAllWithSidoCode(SigunguPageRequest request) {
        var pageRequest = SigunguDtoMapper.toPageRequest(request);
        if (request.getSidoCode() == null) {
            return entityService.getAll(pageRequest)
                    .map(SigunguEntityMapper::toDto);
        } else {
            return entityService.getAllBySidoCode(request.getSidoCode(), pageRequest)
                    .map(SigunguEntityMapper::toDto);
        }
    }
}
