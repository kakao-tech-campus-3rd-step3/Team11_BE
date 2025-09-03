package com.pnu.momeet.domain.profile.service;

import com.pnu.momeet.domain.profile.dto.ProfileCreateRequest;
import com.pnu.momeet.domain.profile.dto.ProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.mapper.EntityMapper;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(UUID memberId) {
        Profile profile = profileRepository.findByMemberId(memberId)
            .orElseThrow(() -> new NoSuchElementException("프로필이 존재하지 않습니다."));
        return EntityMapper.toResponseDto(profile);
    }

    public ProfileResponse createMyProfile(UUID memberId, ProfileCreateRequest request) {
        if (profileRepository.existsByMemberId(memberId)) {
            throw new IllegalStateException("프로필이 이미 존재합니다.");
        }
        if (profileRepository.existsByNicknameIgnoreCase(request.nickname().trim())) {
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
        }

        Profile newProfile = Profile.create(
            memberId,
            request.nickname(),
            request.age(),
            Gender.valueOf(request.gender().toUpperCase()),
            request.imageUrl(),
            request.description(),
            request.baseLocation()
        );

        return EntityMapper.toResponseDto(profileRepository.save(newProfile));
    }
}
