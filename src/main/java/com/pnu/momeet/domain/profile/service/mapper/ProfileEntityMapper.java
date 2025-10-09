package com.pnu.momeet.domain.profile.service.mapper;

import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;

public class ProfileEntityMapper {

    private ProfileEntityMapper() {}

    public static ProfileResponse toResponseDto(Profile profile) {
        return new ProfileResponse(
            profile.getId(),
            profile.getNickname(),
            profile.getAge(),
            profile.getGender(),
            profile.getImageUrl(),
            profile.getDescription(),
            profile.getBaseLocation().getId(),
            profile.getBaseLocation().getSidoName() + " " + profile.getBaseLocation().getSigunguName(),
            profile.getTemperature(),
            profile.getLikes(),
            profile.getDislikes(),
            profile.getCreatedAt(),
            profile.getUpdatedAt()
        );
    }
}
