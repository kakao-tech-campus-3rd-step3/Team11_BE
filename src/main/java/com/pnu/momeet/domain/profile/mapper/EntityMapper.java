package com.pnu.momeet.domain.profile.mapper;

import com.pnu.momeet.domain.profile.dto.ProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;

public class EntityMapper {

    private EntityMapper() {}

    public static ProfileResponse toResponseDto(Profile profile) {
        return new ProfileResponse(
            profile.getId(),
            profile.getNickname(),
            profile.getAge(),
            profile.getGender(),
            profile.getImageUrl(),
            profile.getDescription(),
            profile.getBaseLocation(),
            profile.getTemperature(),
            profile.getLikes(),
            profile.getDislikes(),
            profile.getUnEvaluatedMeetupId(),
            profile.getCreatedAt(),
            profile.getUpdatedAt()
        );
    }
}
