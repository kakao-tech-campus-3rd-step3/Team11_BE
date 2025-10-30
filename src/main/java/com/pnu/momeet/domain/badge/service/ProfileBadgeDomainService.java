package com.pnu.momeet.domain.badge.service;

import aj.org.objectweb.asm.commons.Remapper;
import com.pnu.momeet.domain.badge.dto.request.BadgeAwardRequest;
import com.pnu.momeet.domain.badge.dto.request.ProfileBadgePageRequest;
import com.pnu.momeet.domain.badge.dto.request.ProfileBadgeRepresentativeRequest;
import com.pnu.momeet.domain.badge.dto.response.BadgeAwardResponse;
import com.pnu.momeet.domain.badge.dto.response.ProfileBadgeResponse;
import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.entity.ProfileBadge;
import com.pnu.momeet.domain.badge.service.mapper.ProfileBadgeDtoMapper;
import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
import com.pnu.momeet.domain.profile.service.ProfileDomainService;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
import java.util.Optional;
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
public class ProfileBadgeDomainService {

    private final ProfileBadgeEntityService entityService;
    private final ProfileDomainService profileService;
    private final BadgeDomainService badgeService;
    private final BadgeAwardService badgeAwardService;

    @Transactional(readOnly = true)
    public Page<ProfileBadgeResponse> getMyBadges(UUID memberId, ProfileBadgePageRequest request) {
        PageRequest pageRequest = request.toPageRequest();
        ProfileResponse profile = profileService.getProfileByMemberId(memberId);
        log.debug("내 배지 조회. memberId={}, profileId={}", memberId, profile.id());
        return entityService.findBadgesByProfileId(profile.id(), pageRequest);
    }

    @Transactional(readOnly = true)
    public Page<ProfileBadgeResponse> getUserBadges(UUID profileId, ProfileBadgePageRequest request) {
        PageRequest pageRequest = request.toPageRequest();
        profileService.getProfileById(profileId); // 존재 검증
        log.debug("특정 사용자 배지 조회. profileId={}", profileId);
        return entityService.findBadgesByProfileId(profileId, pageRequest);
    }

    @Transactional(readOnly = true)
    public Optional<ProfileBadgeResponse> getMyRepresentativeBadge(UUID memberId) {
        log.debug("내 대표 배지 조회 시도. memberId={}", memberId);
        ProfileResponse profile = profileService.getProfileByMemberId(memberId);
        Optional<ProfileBadgeResponse> representative = entityService
            .getRepresentativeByProfileId(profile.id());
        log.debug("내 대표 배지 조회 완료. memberId={}, representative={}", memberId, representative);
        return representative;
    }

    @Transactional(readOnly = true)
    public Optional<ProfileBadgeResponse> getUserRepresentativeBadge(UUID profileId) {
        log.debug("특정 사용자 대표 배지 조회 시도. profileId={}", profileId);
        profileService.getProfileById(profileId); // 존재 검증
        Optional<ProfileBadgeResponse> representative = entityService
            .getRepresentativeByProfileId(profileId);
        log.debug("특정 사용자 대표 배지 조회 완료. profileId={}, representative={}", profileId, representative);
        return representative;
    }

    @Transactional
    public ProfileBadgeResponse setRepresentativeBadge(
        UUID memberId,
        ProfileBadgeRepresentativeRequest request
    ) {
        log.debug("대표 배지 설정 시도. memberId={}, badgeId={}", memberId, request.badgeId());
        ProfileResponse profile = profileService.getProfileByMemberId(memberId);
        Badge badge = badgeService.getById(request.badgeId());

        if (!entityService.existsByProfileIdAndBadgeId(profile.id(), request.badgeId())) {
            log.info("존재하지 않는 프로필 배지. profileId={}, badgeId={}", profile.id(), request.badgeId());
            throw new NoSuchElementException("존재하지 않는 프로필 배지입니다.");
        }

        if (entityService.isRepresentative(profile.id(), request.badgeId())) {
            log.info("이미 대표 배지로 설정된 배지. profileId={}, badgeId={}", profile.id(),
                request.badgeId());
            ProfileBadge profileBadge = entityService.getByProfileIdAndBadgeId(profile.id(), request.badgeId());
            return ProfileBadgeDtoMapper.toProfileBadgeResponse(profileBadge, badge);
        }

        entityService.resetRepresentative(profile.id());
        entityService.setRepresentative(profile.id(), request.badgeId());

        ProfileBadge profileBadge = entityService.getByProfileIdAndBadgeId(profile.id(), request.badgeId());
        log.debug("대표 배지 설정 완료. profileId={}, badgeId={}", profile.id(), request.badgeId());
        return ProfileBadgeDtoMapper.toProfileBadgeResponse(profileBadge, badge);
    }

    @Transactional
    public BadgeAwardResponse award(UUID targetProfileId, BadgeAwardRequest request) {
        String rawCode = request.code();
        String normalizedCode = rawCode == null ? null : rawCode.trim().toUpperCase();
        Badge badge = badgeService.getByCode(normalizedCode);
        ProfileResponse profile = profileService.getProfileById(targetProfileId);

        badgeAwardService.award(profile.id(), badge.getCode());

        log.debug("배지 부여 완료. profileId={}, badgeId={}", profile.id(), badge.getId());
        ProfileBadge profileBadge = entityService.getByProfileIdAndBadgeId(profile.id(), badge.getId());
        return ProfileBadgeDtoMapper.toBadgeAwardResponse(profileBadge, badge);
    }
}
