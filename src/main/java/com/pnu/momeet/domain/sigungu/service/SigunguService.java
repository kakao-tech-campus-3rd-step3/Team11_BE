package com.pnu.momeet.domain.sigungu.service;

import com.pnu.momeet.domain.sigungu.dto.request.SigunguPageRequest;
import com.pnu.momeet.domain.sigungu.dto.response.SigunguResponse;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import com.pnu.momeet.domain.sigungu.repository.SigunguRepository;
import com.pnu.momeet.domain.sigungu.service.mapper.SigunguDtoMapper;
import com.pnu.momeet.domain.sigungu.service.mapper.SigunguEntityMapper;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class SigunguService {
    private final SigunguRepository sigunguRepository;
    private final GeometryFactory geometryFactory;

    @Transactional(readOnly = true)
    public SigunguResponse findById(Long id) {
        return SigunguEntityMapper.toDto(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public Sigungu findEntityById(Long id) {
        return sigunguRepository.findById(id).orElseThrow(
            () -> new NoSuchElementException("해당 id의 시군구가 존재하지 않습니다. id=" + id)
        );
    }

    @Transactional(readOnly = true)
    public Sigungu findEntityByPointIn(Point point) {
        return sigunguRepository.findByPointIn(point).orElseThrow(
                () -> new NoSuchElementException("해당 좌표의 시군구가 존재하지 않습니다. point=" + point)
        );
    }

    @Transactional(readOnly = true)
    public SigunguResponse findByPointIn(Point point) {
        return SigunguEntityMapper.toDto(findEntityByPointIn(point));
    }

    @Transactional(readOnly = true)
    public SigunguResponse findByPointIn(double latitude, double longitude) {
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        return findByPointIn(point);
    }

    @Transactional(readOnly = true)
    public Page<SigunguResponse> findAll(Pageable pageable) {
        return sigunguRepository.findAll(pageable)
                .map(SigunguEntityMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<SigunguResponse> findAllBySidoCode(Long sidoCode, Pageable pageable) {
        return sigunguRepository.findAllBySidoCode(sidoCode, pageable)
                .map(SigunguEntityMapper::toDto);
    }

    public Page<SigunguResponse> findAllWithSidoCode(SigunguPageRequest request) {
        if (request.getSidoCode() == null) {
            return findAll(SigunguDtoMapper.toPageRequest(request));
        }
        return findAllBySidoCode(
            request.getSidoCode(), 
            SigunguDtoMapper.toPageRequest(request));
    }
}
