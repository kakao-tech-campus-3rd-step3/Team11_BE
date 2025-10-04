package com.pnu.momeet.domain.profile.service;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.profile.dto.request.ProfileCreateRequest;
import com.pnu.momeet.domain.profile.dto.request.ProfileUpdateRequest;
import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.service.mapper.ProfileDtoMapper;
import com.pnu.momeet.domain.profile.service.mapper.ProfileEntityMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileDomainService {
    
    private static final String PROFILE_IMAGE_PREFIX = "/profiles";

    private final ProfileEntityService entityService;
    private final S3StorageService s3StorageService;

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(UUID memberId) {
        return ProfileEntityMapper.toResponseDto(entityService.getByMemberId(memberId));
    }

    @Deprecated
    @Transactional(readOnly = true)
    public Profile getProfileEntityByMemberId(UUID memberId) {
        return entityService.getByMemberId(memberId);
    }

    @Deprecated
    @Transactional(readOnly = true)
    public Profile getProfileEntityByProfileId(UUID profileId) {
        return entityService.getById(profileId);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfileByMemberId(UUID memberId) {
        return ProfileEntityMapper.toResponseDto(entityService.getByMemberId(memberId));
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(UUID profileId) {
        return ProfileEntityMapper.toResponseDto(entityService.getById(profileId));
    }

    @Transactional
    public ProfileResponse createMyProfile(UUID memberId, ProfileCreateRequest request) {
        if (entityService.existsByMemberId(memberId)) {
            log.warn("이미 존재하는 프로필로 프로필 생성 시도. memberId={}", memberId);
            throw new IllegalStateException("프로필이 이미 존재합니다.");
        }
        if (entityService.existsByNicknameIgnoreCase(request.nickname().trim())) {
            log.info("이미 존재하는 닉네임으로 프로필 생성 시도. memberId={}, nickname={}", memberId, request.nickname());
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
        }
        String profileImageUrl = null;
        if (request.image() != null) {
            profileImageUrl = s3StorageService.uploadImage(request.image(), PROFILE_IMAGE_PREFIX);
        }
        Profile newProfile = ProfileDtoMapper.toEntity(request, profileImageUrl, memberId);
        newProfile = entityService.createProfile(newProfile);
        log.info("새로운 프로필 생성 성공. id={}, memberId={}", newProfile.getId(), memberId);
        return ProfileEntityMapper.toResponseDto(newProfile);
    }

    @Transactional
    public ProfileResponse updateMyProfile(UUID memberId, ProfileUpdateRequest request) {
        Profile profile = entityService.getByMemberId(memberId);
        if (request.nickname() != null && entityService.existsByNicknameIgnoreCase(request.nickname().trim())) {
            log.info("이미 존재하는 닉네임으로 프로필 수정 시도. id={}, nickname={}", profile.getId(), profile.getNickname());
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다. nickname=" + profile.getNickname());
        }

        profile = entityService.updateProfile(profile, p -> p.updateProfile(
            request.nickname(),
            request.age(),
            request.gender() != null ? Gender.valueOf(request.gender().toUpperCase()) : null,
            request.description(),
            request.baseLocation()
        ));

        if (request.image() != null && !request.image().isEmpty()) {
            // 1. 기존 이미지가 있다면 S3에서 삭제
            if (profile.getImageUrl() != null) {
                s3StorageService.deleteImage(profile.getImageUrl());
            }
            // 2. 새 이미지 업로드 및 URL 업데이트
            String newImageUrl = s3StorageService.uploadImage(request.image(), PROFILE_IMAGE_PREFIX);
            profile.updateImageUrl(newImageUrl);
            log.info("프로필 이미지 변경 감지. id={}, memberId={}", profile.getId(), memberId);
        }

        log.info("프로필 수정 성공. id={}, memberId={}", profile.getId(), memberId);
        return ProfileEntityMapper.toResponseDto(profile);
    }
}
