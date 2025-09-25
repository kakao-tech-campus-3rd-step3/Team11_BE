package com.pnu.momeet.domain.participant.service.mapper;

import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.enums.MeetupRole;
import com.pnu.momeet.domain.profile.entity.Profile;

public class ParticipantDtoMapper {
    private ParticipantDtoMapper() {

    }

    public static Participant toEntity(Profile profile, Meetup meetup, MeetupRole role) {
        return Participant.builder()
                .profile(profile)
                .meetup(meetup)
                .role(role)
                .build();
    }
}
