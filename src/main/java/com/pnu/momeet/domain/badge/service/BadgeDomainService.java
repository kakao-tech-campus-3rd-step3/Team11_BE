package com.pnu.momeet.domain.badge.service;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.badge.dto.request.BadgeCreateRequest;
import com.pnu.momeet.domain.badge.dto.request.BadgePageRequest;
import com.pnu.momeet.domain.badge.dto.request.BadgeUpdateRequest;
import com.pnu.momeet.domain.badge.dto.request.ProfileBadgePageRequest;
import com.pnu.momeet.domain.badge.dto.response.BadgeCreateResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeUpdateResponse;
import com.pnu.momeet.domain.badge.dto.response.ProfileBadgeResponse;
import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.service.mapper.BadgeDtoMapper;
import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
import com.pnu.momeet.domain.profile.service.ProfileDomainService;
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
public class BadgeDomainService {

    private static final String ICON_IMAGE_PREFIX = "/badges";

    private final BadgeEntityService entityService;
    private final ProfileDomainService profileService;
    private final S3StorageService s3StorageService;

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

    @Transactional
    public BadgeCreateResponse createBadge(BadgeCreateRequest request) {
        String name = request.name().trim();
        if (entityService.existsByNameIgnoreCase(name)) {
            log.warn("중복 이름으로 배지 생성 시도. name={}", name);
            throw new IllegalArgumentException("이미 존재하는 배지 이름입니다.");
        }

        String rawCode = request.code();
        String normalizedCode = rawCode == null ? null : rawCode.trim().toUpperCase();
        if (entityService.existsByCodeIgnoreCase(normalizedCode)) {
            log.warn("중복 code로 배지 생성 시도. code={}", normalizedCode);
            throw new IllegalArgumentException("이미 존재하는 배지 코드입니다.");
        }

        String iconUrl = null;
        if (request.iconImage() != null) {
            iconUrl = s3StorageService.uploadImage(request.iconImage(), ICON_IMAGE_PREFIX);
            log.info("배지 아이콘 업로드 성공. name={}, iconUrl={}", name, iconUrl);
        }

        Badge newBadge = Badge.create(name, request.description(), iconUrl, request.code());
        Badge saved = entityService.save(newBadge);
        log.info("배지 생성 성공. id={}, name={}", saved.getId(), saved.getName());
        return BadgeDtoMapper.toCreateResponseDto(saved);
    }

    @Transactional
    public BadgeUpdateResponse updateBadge(UUID badgeId, BadgeUpdateRequest request) {
        Badge badge = entityService.getById(badgeId);

        // 이름 중복 검증 (이름이 변경되는 경우에만)
        if (request.name() != null && !badge.getName().equalsIgnoreCase(request.name().trim())) {
            if (entityService.existsByNameIgnoreCase(request.name().trim())) {
                log.warn("중복 이름으로 배지 수정 시도. id={}, newName={}", badgeId, request.name());
                throw new IllegalArgumentException("이미 존재하는 배지 이름입니다.");
            }
        }

        // 아이콘 교체
        String oldImageUrl = badge.getIconUrl();
        if (request.iconImage() != null && !request.iconImage().isEmpty()) {
            String newImageUrl = s3StorageService.uploadImage(request.iconImage(), ICON_IMAGE_PREFIX);
            badge.updateIconUrl(newImageUrl);
            log.info("배지 아이콘 변경. id={}, newIconUrl={}", badgeId, newImageUrl);
            s3StorageService.deleteImage(oldImageUrl);
            log.debug("배지 기존 아이콘 삭제 완료. id={}, oldIconUrl={}", badgeId, oldImageUrl);
        }

        // 텍스트 정보 변경
        badge.updateBadge(request.name(), request.description());
        log.info("배지 수정 성공. id={}, name={}", badge.getId(), badge.getName());

        return BadgeDtoMapper.toUpdateResponseDto(badge);
    }

    @Transactional
    public void deleteBadge(UUID badgeId) {
        Badge badge = entityService.getById(badgeId);
        String iconUrl = badge.getIconUrl();

        entityService.delete(badge);
        log.info("배지 삭제 성공. id={}", badgeId);

        s3StorageService.deleteImage(iconUrl);
        log.debug("배지 아이콘 객체 삭제 완료. id={}, iconUrl={}", badgeId, iconUrl);
    }

    @Transactional(readOnly = true)
    public Page<BadgeResponse> getBadges(BadgePageRequest request) {
        var pageRequest = request.toPageRequest();
        var page = entityService.findAll(pageRequest);
        log.debug("배지 목록 조회. page={}, size={}, total={}",
            pageRequest.getPageNumber(), pageRequest.getPageSize(), page.getTotalElements());
        return page.map(BadgeDtoMapper::toBadgeResponse);
    }
}
