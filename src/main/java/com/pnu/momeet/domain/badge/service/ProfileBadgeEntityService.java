package com.pnu.momeet.domain.badge.service;

import com.pnu.momeet.domain.badge.dto.response.ProfileBadgeResponse;
import com.pnu.momeet.domain.badge.entity.ProfileBadge;
import com.pnu.momeet.domain.badge.repository.ProfileBadgeDslRepository;
import com.pnu.momeet.domain.badge.repository.ProfileBadgeRepository;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileBadgeEntityService {

    private final ProfileBadgeDslRepository profileBadgeDslRepository;
    private final ProfileBadgeRepository profileBadgeRepository;

    @Transactional(readOnly = true)
    public ProfileBadge getByProfileIdAndBadgeId(UUID profileId, UUID badgeId) {
        log.debug("프로필 배지 단건 조회 시도. profileId={}, badgeId={}", profileId, badgeId);
        return profileBadgeRepository.findByProfileIdAndBadgeId(profileId, badgeId)
            .orElseThrow(() -> {
                log.info("존재하지 않는 프로필 배지 조회 시도. profileId={}, badgeId={}", profileId, badgeId);
                return new NoSuchElementException("존재하지 않는 프로필 배지입니다.");
            });
    }

    @Transactional(readOnly = true)
    public Page<ProfileBadgeResponse> findBadgesByProfileId(UUID profileId, Pageable pageable) {
        log.debug("프로필 배지 페이지 조회. profileId={}, page={}, size={}",
            profileId, pageable.getPageNumber(), pageable.getPageSize());
        return profileBadgeDslRepository.findBadgesByProfileId(profileId, pageable);
    }

    @Transactional(readOnly = true)
    public boolean existsByProfileIdAndBadgeId(UUID profileId, UUID badgeId) {
        log.debug("프로필 배지 존재 여부 조회. profileId={}, badgeId={}", profileId, badgeId);
        return profileBadgeRepository.existsByProfileIdAndBadgeId(profileId, badgeId);
    }

    @Transactional(readOnly = true)
    public boolean isRepresentative(UUID profileId, UUID badgeId) {
        log.debug("프로필 배지 대표 여부 조회. profileId={}, badgeId={}", profileId, badgeId);
        return profileBadgeDslRepository.isRepresentative(profileId, badgeId);
    }

    @Transactional
    public void resetRepresentative(UUID profileId) {
        log.debug("대표 배지 초기화 시도. profileId={}", profileId);
        long reset = profileBadgeDslRepository.resetRepresentative(profileId);
        log.debug("대표 배지 초기화 영향 받은 행 수: reset={}", reset);
        log.debug("대표 배지 초기화 완료. profileId={}", profileId);
    }

    @Transactional
    public void setRepresentative(UUID profileId, UUID badgeId) {
        log.debug("대표 배지 설정 시도. profileId={}, badgeId={}", profileId, badgeId);
        long updated = profileBadgeDslRepository.setRepresentative(profileId, badgeId);
        if (updated == 0) {
            log.info("대표 배지 설정 실패. profileId={}, badgeId={}", profileId, badgeId);
            throw new IllegalStateException("대표 배지 설정에 실패했습니다.");
        }
        log.debug("대표 배지 설정 완료. profileId={}, badgeId={}", profileId, badgeId);
    }
}
