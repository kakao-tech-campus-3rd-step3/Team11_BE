package com.pnu.momeet.domain.sigungu.mapper;

import com.pnu.momeet.domain.sigungu.dto.BaseLocationResponse;
import com.pnu.momeet.domain.sigungu.dto.SigunguResponse;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import org.locationtech.jts.geom.Point;

public class EntityMapper {

    private EntityMapper() {
        // private constructor to prevent instantiation
    }

    public static SigunguResponse toDto(Sigungu sigungu) {
        Point baseLocation = sigungu.getBaseLocation();
        return new SigunguResponse(
                sigungu.getId(),
                sigungu.getSidoName(),
                sigungu.getSidoCode(),
                sigungu.getSigunguName(),
                sigungu.getSigunguCode(),
                new BaseLocationResponse(
                        baseLocation.getX(),
                        baseLocation.getY()
                ),
                sigungu.getCreatedAt(),
                sigungu.getUpdatedAt()
        );

    }
}
