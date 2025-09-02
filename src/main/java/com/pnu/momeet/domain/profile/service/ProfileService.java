package com.pnu.momeet.domain.profile.service;

import com.pnu.momeet.domain.profile.dto.ProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.mapper.EntityMapper;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
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
}
