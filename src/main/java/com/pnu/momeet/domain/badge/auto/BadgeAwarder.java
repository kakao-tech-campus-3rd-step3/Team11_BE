package com.pnu.momeet.domain.badge.auto;

import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.entity.ProfileBadge;
import com.pnu.momeet.domain.badge.repository.BadgeRepository;
import com.pnu.momeet.domain.badge.repository.ProfileBadgeRepository;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeAwarder {

    private final BadgeRepository badgeRepository;
    private final ProfileBadgeRepository profileBadgeRepository;

    @Transactional
    public void award(UUID profileId, String code) {
        String normalized = code == null ? null : code.trim().toUpperCase();
        Badge badge = badgeRepository.findByCodeIgnoreCase(normalized)
            .orElseThrow(() -> new NoSuchElementException("배지 코드 매핑 누락: " + normalized));
        try {
            profileBadgeRepository.save(new ProfileBadge(profileId, badge.getId()));
        } catch (DataIntegrityViolationException e) {
            // UNIQUE (profile_id, badge_id) 제약 조건 위반 → 중복 배지 부여 무시
            log.debug("중복 배지 부여 시도 무시. profileId={}, badgeId={}", profileId, badge.getId());
        }
    }
}
