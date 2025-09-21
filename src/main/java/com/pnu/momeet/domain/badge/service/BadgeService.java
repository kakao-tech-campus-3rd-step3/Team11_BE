package com.pnu.momeet.domain.badge.service;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.badge.dto.request.BadgeCreateRequest;
import com.pnu.momeet.domain.badge.dto.request.BadgePageRequest;
import com.pnu.momeet.domain.badge.dto.request.BadgeUpdateRequest;
import com.pnu.momeet.domain.badge.dto.response.BadgeCreateResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeUpdateResponse;
import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.repository.BadgeDslRepository;
import com.pnu.momeet.domain.badge.repository.BadgeRepository;
import com.pnu.momeet.domain.badge.service.mapper.BadgeDtoMapper;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileService;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private static final String ICON_IMAGE_PREFIX = "/badges";

    private final BadgeDslRepository badgeDslRepository;
    private final BadgeRepository badgeRepository;
    private final ProfileService profileService;
    private final S3StorageService s3StorageService;

    @Transactional(readOnly = true)
    public Page<BadgeResponse> getMyBadges(UUID memberId, BadgePageRequest badgePageRequest) {
        PageRequest pageRequest = BadgeDtoMapper.toPageRequest(badgePageRequest);
        Profile profile = profileService.getProfileEntityByMemberId(memberId);
        return badgeDslRepository.findBadgesByProfileId(profile.getId(), pageRequest);
    }

    @Transactional(readOnly = true)
    public Page<BadgeResponse> getUserBadges(UUID profileId, BadgePageRequest request) {
        PageRequest pageRequest = BadgeDtoMapper.toPageRequest(request);
        // 프로필 존재 검증
        profileService.getProfileEntityByProfileId(profileId);
        return badgeDslRepository.findBadgesByProfileId(profileId, pageRequest);
    }

    @Transactional
    public BadgeCreateResponse createBadge(BadgeCreateRequest request) {
        if (badgeRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new IllegalArgumentException("이미 존재하는 배지 이름입니다.");
        }

        String iconUrl = null;
        if (request.iconImage() != null) {
            iconUrl = s3StorageService.uploadImage(request.iconImage(), ICON_IMAGE_PREFIX);
        }

        Badge newBadge = Badge.create(request.name(), request.description(), iconUrl);

        return BadgeDtoMapper.toCreateResponseDto(badgeRepository.save(newBadge));
    }

    @Transactional
    public BadgeUpdateResponse updateBadge(UUID badgeId, BadgeUpdateRequest request) {
        Badge badge = badgeRepository.findById(badgeId)
            .orElseThrow(() -> new NoSuchElementException("존재하지 않는 배지입니다."));

        if (request.name() != null && !badge.getName().equalsIgnoreCase(request.name().trim())) {
            if (badgeRepository.existsByNameIgnoreCase(request.name())) {
                throw new IllegalArgumentException("이미 존재하는 배지 이름입니다.");
            }
        }

        String oldImageUrl = badge.getIconUrl();
        if (request.iconImage() != null && !request.iconImage().isEmpty()) {
            // 1. 새 이미지 업로드 및 URL 업데이트
            String newImageUrl = s3StorageService.uploadImage(request.iconImage(), ICON_IMAGE_PREFIX);
            badge.updateIconUrl(newImageUrl);
            // 2. 기존 이미지가 있다면 S3에서 삭제
            if (badge.getIconUrl() != null) {
                s3StorageService.deleteImage(oldImageUrl);
            }
        }

        badge.updateBadge(request.name(), request.description());

        return BadgeDtoMapper.toUpdateResponseDto(badge);
    }
}
