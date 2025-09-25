package com.pnu.momeet.domain.meetup.service.mapper;

import com.pnu.momeet.domain.common.dto.response.LocationResponse;
import com.pnu.momeet.domain.meetup.dto.response.MeetupDetail;
import com.pnu.momeet.domain.meetup.dto.response.MeetupResponse;
import com.pnu.momeet.domain.meetup.dto.response.UnEvaluatedMeetupDto;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.entity.MeetupHashTag;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.enums.MeetupRole;
import com.pnu.momeet.domain.profile.service.mapper.ProfileEntityMapper;
import com.pnu.momeet.domain.sigungu.service.mapper.SigunguEntityMapper;

import java.util.List;

public class MeetupEntityMapper {

    private MeetupEntityMapper() {
        // private constructor to prevent instantiation
    }


    public static MeetupResponse toResponse(Meetup meetup) {
        LocationResponse location = new LocationResponse(
                meetup.getLocationPoint().getX(),
                meetup.getLocationPoint().getY(),
                meetup.getAddress()
        );

        List<String> hashTags = meetup.getHashTags().stream()
                .map(MeetupHashTag::getName)
                .toList();

        return new MeetupResponse(
                meetup.getId(),
                meetup.getOwner().getId(),
                meetup.getSigungu().getId(),
                meetup.getName(),
                meetup.getCategory().name(),
                meetup.getSubCategory().name(),
                hashTags,
                meetup.getParticipantCount(),
                meetup.getCapacity(),
                meetup.getScoreLimit(),
                location,
                meetup.getStatus().name(),
                meetup.getEndAt(),
                meetup.getCreatedAt(),
                meetup.getUpdatedAt()
        );
    }

    public static MeetupDetail toDetail(Meetup meetup) {
        LocationResponse location = new LocationResponse(
                meetup.getLocationPoint().getX(),
                meetup.getLocationPoint().getY(),
                meetup.getAddress()
        );

        List<String> hashTags = meetup.getHashTags().stream()
                .map(MeetupHashTag::getName)
                .toList();

        return new MeetupDetail(
                meetup.getId(),
                meetup.getName(),
                meetup.getCategory().name(),
                meetup.getSubCategory().name(),
                meetup.getDescription(),
                meetup.getParticipantCount(),
                meetup.getCapacity(),
                meetup.getScoreLimit(),
                ProfileEntityMapper.toResponseDto(meetup.getOwner()),
                SigunguEntityMapper.toDto(meetup.getSigungu()),
                location,
                hashTags,
                meetup.getStatus().name(),
                meetup.getEndAt(),
                meetup.getCreatedAt(),
                meetup.getUpdatedAt()
        );
    }

    public static UnEvaluatedMeetupDto unEvaluatedMeetupDto(Meetup meetup, long unEvaluatedCount) {
        return new UnEvaluatedMeetupDto(
            meetup.getId(),
            meetup.getName(),
            meetup.getCategory().name(),
            meetup.getSubCategory().name(),
            meetup.getCreatedAt(),
            meetup.getEndAt(),
            meetup.getParticipantCount(),
            meetup.getCapacity(),
            unEvaluatedCount
        );
    }

    public static Participant toHostParticipant(Meetup meetup) {
        return Participant.builder()
                .meetup(meetup)
                .profile(meetup.getOwner())
                .role(MeetupRole.HOST)
                .build();
    }
}
