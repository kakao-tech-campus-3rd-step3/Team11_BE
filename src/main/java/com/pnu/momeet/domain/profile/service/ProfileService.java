package com.pnu.momeet.domain.profile.service;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.profile.dto.request.ProfileCreateRequest;
import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
import com.pnu.momeet.domain.profile.dto.request.ProfileUpdateRequest;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.service.mapper.ProfileEntityMapper;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final S3StorageService s3StorageService;

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(UUID memberId) {
        Profile profile = profileRepository.findByMemberId(memberId)
            .orElseThrow(() -> new NoSuchElementException("프로필이 존재하지 않습니다."));
        return ProfileEntityMapper.toResponseDto(profile);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(UUID profileId) {
        Profile profile = profileRepository.findById(profileId)
            .orElseThrow(() -> new NoSuchElementException(
                "ID에 해당하는 프로필을 찾을 수 없습니다: " + profileId
            ));
        return ProfileEntityMapper.toResponseDto(profile);
    }

    @Transactional
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

        return ProfileEntityMapper.toResponseDto(profileRepository.save(newProfile));
    }

    @Transactional
    public ProfileResponse updateMyProfile(UUID memberId, @Valid ProfileUpdateRequest request) {
        Profile profile = profileRepository.findByMemberId(memberId)
            .orElseThrow(() -> new NoSuchElementException("프로필이 존재하지 않습니다."));

        if (request.nickname() != null && !profile.getNickname().equalsIgnoreCase(request.nickname().trim())) {
            if (profileRepository.existsByNicknameIgnoreCase(request.nickname().trim())) {
                throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
            }
        }

        profile.updateProfile(
            request.nickname(),
            request.age(),
            request.gender() != null ? Gender.valueOf(request.gender().toUpperCase()) : null,
            request.imageUrl(),
            request.description(),
            request.baseLocation()
        );

        return ProfileEntityMapper.toResponseDto(profile);
    }

    @Transactional
    public ProfileResponse updateProfileImageUrl(UUID memberId, MultipartFile multipartFile) {
        Profile profile = profileRepository.findByMemberId(memberId)
            .orElseThrow(() -> new NoSuchElementException("프로필이 존재하지 않습니다."));

        String imageUrl = s3StorageService.uploadImage(multipartFile, "profiles/");

        profile.updateProfile(null, null, null, imageUrl, null, null);

        return ProfileEntityMapper.toResponseDto(profile);
    }

    @Transactional
    public void deleteProfileImageUrl(UUID memberId) {
        Profile profile = profileRepository.findByMemberId(memberId)
            .orElseThrow(() -> new NoSuchElementException("프로필이 존재하지 않습니다."));

        String imageUrl = profile.getImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            // 1) S3에서 실제 객체 삭제 (멱등)
            s3StorageService.deleteImage(imageUrl);
        }

        profile.updateImageUrl(null);
    }
}
