package com.pnu.momeet.domain.sigungu.service.mapper;

import com.pnu.momeet.domain.sigungu.dto.response.BaseLocationResponse;
import com.pnu.momeet.domain.sigungu.dto.response.SigunguResponse;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import org.locationtech.jts.geom.Point;

public class SigunguEntityMapper {

    private SigunguEntityMapper() {
        // private constructor to prevent instantiation
    }

    public static SigunguResponse toDto(Sigungu sigungu) {
        Point baseLocation = sigungu.getBaseLocation();
        return new SigunguResponse(
                sigungu.getSidoName(),
                sigungu.getSidoCode(),
                sigungu.getSigunguName(),
                sigungu.getId(),
                new BaseLocationResponse(
                        baseLocation.getX(),
                        baseLocation.getY()
                ),
                sigungu.getCreatedAt(),
                sigungu.getUpdatedAt()
        );

    }
}
