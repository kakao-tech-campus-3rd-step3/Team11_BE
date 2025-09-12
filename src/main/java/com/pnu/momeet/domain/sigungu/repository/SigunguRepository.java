package com.pnu.momeet.domain.sigungu.repository;

import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SigunguRepository extends JpaRepository<Sigungu, Long> {

    @Query("SELECT s FROM Sigungu s WHERE ST_Contains(s.area, :point) = true")
    Optional<Sigungu> findByPointIn(Point point);

    Page<Sigungu> findAllBySidoCode(Long sidoCode, Pageable pageable);
}
