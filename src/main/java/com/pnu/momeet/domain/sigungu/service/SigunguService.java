package com.pnu.momeet.domain.sigungu.service;

import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import com.pnu.momeet.domain.sigungu.repository.SigunguRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SigunguService {
    private final SigunguRepository sigunguRepository;
    private final GeometryFactory geometryFactory;

    public Sigungu findById(Long id) {
        return sigunguRepository.findById(id).orElseThrow(
            () -> new IllegalArgumentException("해당 id의 시군구가 존재하지 않습니다. id=" + id)
        );
    }

    public Sigungu findByPointIn(double latitude, double longitude) {
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));

        return sigunguRepository.findByPointIn(point).orElseThrow(
            () -> new IllegalArgumentException("해당 좌표의 시군구가 존재하지 않습니다. point=" + point)
        );
    }

    public Page<Sigungu> findAll(PageRequest pageable) {
        return sigunguRepository.findAllBy(pageable);
    }

    public Page<Sigungu> findAllBySidoCode(Long sidoCode, PageRequest pageable) {
        return sigunguRepository.findAllBySidoCode(sidoCode, pageable);
    }
}
