package com.pnu.momeet.domain.sigungu.service;

import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import com.pnu.momeet.domain.sigungu.repository.SigunguRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SigunguEntityService {
    private final SigunguRepository sigunguRepository;

    @Transactional(readOnly = true)
    public Sigungu getByPointIn(Point point) {
        log.debug("특정 좌표의 시군구 조회 시도. point={}", point);

        Optional<Sigungu> sigungu =  sigunguRepository.findByPointIn(point);
        if (sigungu.isEmpty()) {
            log.info("존재하지 않는 좌표의 시군구 조회 시도. point={}", point);
            throw new NoSuchElementException("해당 좌표의 시군구가 존재하지 않습니다. point=" + point);
        }

        log.debug("특정 좌표의 시군구 조회 성공. sgg_code={}", sigungu.get().getId());
        return sigungu.get();
    }

    @Transactional(readOnly = true)
    public Sigungu getById(Long id) {
        log.debug("특정 id의 시군구 조회 시도. id={}", id);

        Optional<Sigungu> sigungu = sigunguRepository.findById(id);
        if (sigungu.isEmpty()) {
            log.info("존재하지 않는 id의 시군구 조회 시도. id={}", id);
            throw new NoSuchElementException("해당 id의 시군구가 존재하지 않습니다. id=" + id);
        }

        log.debug("특정 id의 시군구 조회 성공. sgg_code={}", id);
        return sigungu.get();
    }

    @Transactional(readOnly = true)
    public Sigungu getBySidoNameAndSigunguName(String sidoName, String sigunguName) {
        log.debug("특정 이름의 시군구 조회 시도. sidoName={}, sigunguName={}", sidoName, sigunguName);

        Optional<Sigungu> sigungu = sigunguRepository.findBySidoNameAndSigunguName(sidoName, sigunguName);
        if (sigungu.isEmpty()) {
            log.info("존재하지 않는 이름의 시군구 조회 시도. sidoName={}, sigunguName={}", sidoName, sigunguName);
            throw new NoSuchElementException("해당 이름의 시군구가 존재하지 않습니다. sidoName=" + sidoName + ", sigunguName=" + sigunguName);
        }

        log.debug("특정 이름의 시군구 조회 성공. sidoName={}, sigunguName={}", sidoName, sigunguName);
        return sigungu.get();
    }

    @Transactional(readOnly = true)
    public Page<Sigungu> getAll(Pageable pageable) {
        log.debug("시군구 전체 조회 시도. pageable={}", pageable);
        var response =  sigunguRepository.findAll(pageable);
        log.debug("시군구 전체 조회 성공. responseSize={}", response.getNumberOfElements());
        return response;
    }

    @Transactional(readOnly = true)
    public Page<Sigungu> getAllBySidoCode(Long sidoCode, Pageable pageable) {
        log.debug("특정 시도 코드의 시군구 조회 시도. sidoCode={}, pageable={}", sidoCode, pageable);
        var response = sigunguRepository.findAllBySidoCode(sidoCode, pageable);
        log.debug("특정 시도 코드의 시군구 조회 성공. sidoCode={}, responseSize={}", sidoCode, response.getNumberOfElements());
        return response;
    }
}
