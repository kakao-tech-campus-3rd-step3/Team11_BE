package com.pnu.momeet.domain.badge.service;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.common.tx.AfterCommitExecutor;
import com.pnu.momeet.common.util.ImageHashUtil;
import com.pnu.momeet.domain.badge.command.BadgeChanges;
import com.pnu.momeet.domain.badge.dto.request.BadgeCreateRequest;
import com.pnu.momeet.domain.badge.dto.request.BadgePageRequest;
import com.pnu.momeet.domain.badge.dto.request.BadgeUpdateRequest;
import com.pnu.momeet.domain.badge.dto.response.BadgeCreateResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeResponse;
import com.pnu.momeet.domain.badge.dto.response.BadgeUpdateResponse;
import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.service.mapper.BadgeDtoMapper;
import com.pnu.momeet.domain.profile.service.ProfileDomainService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeDomainService {

    private static final String ICON_IMAGE_PREFIX = "/badges";

    private final BadgeEntityService entityService;
    private final S3StorageService s3StorageService;
    private final ImageHashUtil imageHashUtil;
    private final AfterCommitExecutor afterCommitExecutor;

    @Transactional
    public BadgeCreateResponse createBadge(BadgeCreateRequest request) {
        String name = request.name().trim();
        if (entityService.existsByNameIgnoreCase(name)) {
            log.info("중복 이름으로 배지 생성 시도. name={}", name);
            throw new IllegalArgumentException("이미 존재하는 배지 이름입니다.");
        }

        String rawCode = request.code();
        String normalizedCode = rawCode == null ? null : rawCode.trim().toUpperCase();
        if (entityService.existsByCodeIgnoreCase(normalizedCode)) {
            log.info("중복 code로 배지 생성 시도. code={}", normalizedCode);
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
        log.debug("배지 수정 시도. id={}", badgeId);
        Badge badge = entityService.getById(badgeId);

        BadgeChanges changes = change(request, badge);

        // 아무 것도 바뀌지 않으면 바로 반환
        if (changes.nothingToDo()) {
            log.info("배지 수정 건너뜀(변경 없음). id={}", badge.getId());
            return BadgeDtoMapper.toUpdateResponseDto(badge);
        }

        // 이름 중복 검증 (이름이 변경되는 경우에만)
        if (changes.nameChanged() &&
            entityService.existsByNameIgnoreCaseAndIdNot(changes.name(), badge.getId())) {
            log.debug("이미 존재하는 배지 이름으로 배지 수정 시도. id={}, name={}", badgeId, changes.name());
            throw new IllegalArgumentException("이미 존재하는 배지 이름입니다.");
        }

        // 텍스트 정보 변경
        if (changes.textChanged()) {
            badge = entityService.updateBadge(badge, b -> b.updateBadge(
                changes.nameChanged() ? changes.name() : null,
                changes.descChanged() ? changes.description() : null
            ));
        }
        log.info("배지 수정 성공. id={}, name={}", badge.getId(), badge.getName());

        // 아이콘 교체
        if (changes.hasIconPart() && changes.iconChanged()) {
            String oldUrl = badge.getIconUrl();
            String newUrl = s3StorageService.uploadImage(request.iconImage(), ICON_IMAGE_PREFIX);
            badge.updateIcon(newUrl, changes.iconHash());

            afterCommitExecutor.run(() -> {
                if (oldUrl != null) s3StorageService.deleteImage(oldUrl);
            });
            log.info("배지 아이콘 변경 완료. id={}, newUrl={}", badge.getId(), newUrl);
        }

        return BadgeDtoMapper.toUpdateResponseDto(badge);
    }

    private BadgeChanges change(BadgeUpdateRequest request, Badge badge) {
        // 정규화
        String inName = request.name() == null ? null : request.name().trim();
        String inDesc = request.description();

        // 텍스트 변경 감지
        boolean nameChanged = inName != null && !inName.equalsIgnoreCase(badge.getName());
        boolean descChanged = inDesc != null && !inDesc.equals(badge.getDescription());

        // 아이콘 변경 감지
        boolean hasIconPart = request.iconImage() != null && !request.iconImage().isEmpty();
        String incomingHash = null;
        boolean iconChanged = false;
        if (hasIconPart) {
            incomingHash = imageHashUtil.sha256Hex(request.iconImage());
            iconChanged = incomingHash != null && !incomingHash.equals(badge.getIconHash());
        }

        return new BadgeChanges(
            inName, nameChanged,
            inDesc, descChanged,
            hasIconPart, incomingHash, iconChanged
        );
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

    @Transactional(readOnly = true)
    public Badge getById(UUID badgeId) {
        return entityService.getById(badgeId);
    }

    @Transactional(readOnly = true)
    public Badge getByCode(String code) {
        log.debug("배지 코드로 조회. code={}", code);
        Badge badge = entityService.getByCode(code);
        log.debug("배지 조회 완료. id={}, name={}", badge.getId(), badge.getName());
        return badge;
    }
}
