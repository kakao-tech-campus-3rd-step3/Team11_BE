package com.pnu.momeet.domain.badge.service;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.badge.dto.request.BadgeCreateRequest;
import com.pnu.momeet.domain.badge.dto.request.BadgePageRequest;
import com.pnu.momeet.domain.badge.dto.response.BadgeCreateResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeResponse;
import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.repository.BadgeDslRepository;
import com.pnu.momeet.domain.badge.repository.BadgeRepository;
import com.pnu.momeet.domain.badge.service.mapper.BadgeDtoMapper;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileService;
import jakarta.validation.Valid;
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
    public BadgeCreateResponse createBadge(@Valid BadgeCreateRequest request) {
        if (badgeRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException("이미 존재하는 배지 이름입니다.");
        }

        String iconUrl = null;
        if (request.iconImage() != null) {
            iconUrl = s3StorageService.uploadImage(request.iconImage(), ICON_IMAGE_PREFIX);
        }

        Badge newBadge = Badge.create(request.name(), request.description(), iconUrl);

        return BadgeDtoMapper.toCreateResponseDto(badgeRepository.save(newBadge));
    }
}
