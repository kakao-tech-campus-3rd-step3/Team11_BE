package com.pnu.momeet.domain.participant.service.mapper;

import com.pnu.momeet.domain.participant.dto.response.ParticipantResponse;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.profile.service.mapper.ProfileEntityMapper;

public class ParticipantEntityMapper {
    private ParticipantEntityMapper() {}

    public static ParticipantResponse toDto(Participant participant) {
        return new ParticipantResponse(
                participant.getId(),
                ProfileEntityMapper.toResponseDto(participant.getProfile()),
                participant.getRole().name(),
                participant.getIsRated(),
                participant.getLastActiveAt(),
                participant.getCreatedAt()
        );
    }
}
