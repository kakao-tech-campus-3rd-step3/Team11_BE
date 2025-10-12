package com.pnu.momeet.domain.profile.service.mapper;

import com.pnu.momeet.domain.profile.dto.request.ProfileCreateRequest;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;

import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import java.util.UUID;

public class ProfileDtoMapper {
    private ProfileDtoMapper() {

    }

    public static Profile toEntity(ProfileCreateRequest request, Sigungu sigungu, String profileImageUrl, UUID memberId) {
        return Profile.create(
                memberId,
                request.nickname(),
                request.age(),
                Gender.valueOf(request.gender().toUpperCase()),
                profileImageUrl,
                request.description(),
                sigungu
        );
    }
}
