package com.pnu.momeet.domain.badge.service;

import com.pnu.momeet.domain.badge.entity.Badge;
import com.pnu.momeet.domain.badge.repository.BadgeRepository;
import com.pnu.momeet.domain.profile.entity.Profile;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeEntityService {

    private final BadgeRepository badgeRepository;

    @Transactional(readOnly = true)
    public Badge getById(UUID badgeId) {
        log.debug("배지 단건 조회 시도. badgeId={}", badgeId);
        return badgeRepository.findById(badgeId)
            .orElseThrow(() -> {
                log.info("존재하지 않는 배지 조회 시도. badgeId={}", badgeId);
                return new NoSuchElementException("존재하지 않는 배지입니다.");
            });
    }

    @Transactional(readOnly = true)
    public Badge getByCode(String code) {
        log.debug("배지 코드로 조회. code={}", code);
        return badgeRepository.findByCodeIgnoreCase(code)
            .orElseThrow(() -> {
                log.info("존재하지 않는 배지 조회 시도. badgeCode={}", code);
                return new NoSuchElementException("존재하지 않는 배지입니다.");
            });
    }

    @Transactional(readOnly = true)
    public boolean existsByNameIgnoreCase(String name) {
        return badgeRepository.existsByNameIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public boolean existsByNameIgnoreCaseAndIdNot(String name, UUID excludeId) {
        return badgeRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId);
    }

    @Transactional(readOnly = true)
    public boolean existsByCodeIgnoreCase(String code) {
        return badgeRepository.existsByCodeIgnoreCase(code);
    }

    @Transactional
    public Badge save(Badge badge) {
        log.debug("배지 저장 시도. name={}", badge.getName());
        Badge saved = badgeRepository.save(badge);
        log.debug("배지 저장 성공. id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    public Badge updateBadge(Badge badge, Consumer<Badge> updater) {
        log.debug("배지 수정 시도. id={}", badge.getId());
        updater.accept(badge);
        log.debug("프로필 수정 성공. id={}", badge.getId());
        return badge;
    }

    @Transactional
    public void delete(Badge badge) {
        log.debug("배지 삭제 시도. id={}", badge.getId());
        badgeRepository.delete(badge);
        log.debug("배지 삭제 성공. id={}", badge.getId());
    }

    @Transactional(readOnly = true)
    public Page<Badge> findAll(Pageable pageable) {
        log.debug("배지 페이지 조회. page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return badgeRepository.findAll(pageable);
    }
}
