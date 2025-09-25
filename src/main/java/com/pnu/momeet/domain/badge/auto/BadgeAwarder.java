package com.pnu.momeet.domain.badge.auto;

import com.pnu.momeet.domain.badge.repository.BadgeRepository;
import com.pnu.momeet.domain.badge.repository.ProfileBadgeRepository;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BadgeAwarder {

    private final BadgeRepository badgeRepository;
    private final ProfileBadgeRepository profileBadgeRepository;

    @Transactional
    public void award(UUID profileId, String code) {
        String normalized = code == null ? null : code.trim().toUpperCase();
        UUID badgeId = badgeRepository.findIdByCode(normalized)
            .orElseThrow(() -> new NoSuchElementException("배지 코드 매핑 누락: " + normalized));
        profileBadgeRepository.insertIgnore(profileId, badgeId);
    }
}
