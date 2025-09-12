package com.pnu.momeet.domain.meetup.service.mapper;

import com.pnu.momeet.domain.common.dto.response.LocationResponse;
import com.pnu.momeet.domain.meetup.dto.response.MeetupResponse;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.sigungu.service.mapper.SigunguEntityMapper;

public class MeetupEntityMapper {

    private MeetupEntityMapper() {
        // private constructor to prevent instantiation
    }


    public static MeetupResponse toDto(Meetup meetup) {
        LocationResponse location = new LocationResponse(
                meetup.getLocationPoint().getX(),
                meetup.getLocationPoint().getY()
        );
        //TODO: 참가자 수 추후 구현
        return new MeetupResponse(
                meetup.getId(),
                meetup.getName(),
                meetup.getCategory().name(),
                meetup.getSubCategory().name(),
                meetup.getDescription(),
                meetup.getCapacity(),
                meetup.getScoreLimit(),
                SigunguEntityMapper.toDto(meetup.getSigungu()),
                location,
                meetup.getStatus().name(),
                meetup.getEndAt(),
                meetup.getCreatedAt(),
                meetup.getUpdatedAt()
        );
    }
}
