package com.pnu.momeet.domain.profile.service;

import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileEntityService {

    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public Profile getByMemberId(UUID memberId) {
        log.debug("특정 memberId의 프로필 조회 시도. memberId={}", memberId);
        var profile = profileRepository.findByMemberId(memberId);
        if (profile.isEmpty()) {
            log.warn("존재하지 않는 memberId의 프로필 조회 시도. memberId={}", memberId);
            throw new NoSuchElementException("해당 memberId의 프로필이 존재하지 않습니다. memberId=" + memberId);
        }
        log.debug("특정 memberId의 프로필 조회 성공. memberId={}", memberId);
        return profile.get();
    }

    @Transactional(readOnly = true)
    public Profile getById(UUID profileId) {
        log.debug("특정 id의 프로필 조회 시도. id={}", profileId);
        var profile = profileRepository.findById(profileId);
        if (profile.isEmpty()) {
            log.warn("존재하지 않는 id의 프로필 조회 시도. id={}", profileId);
            throw new NoSuchElementException("해당 Id의 프로필이 존재하지 않습니다. id=" + profileId);
        }
        log.debug("특정 id의 프로필 조회 성공. id={}", profileId);
        return profile.get();
    }

    @Transactional(readOnly = true)
    public UUID mapToProfileId(UUID memberId) {
        var profileId = profileRepository.findIdByMemberId(memberId);
        if (profileId.isEmpty()) {
            log.warn("존재하지 않는 memberId의 프로필 ID 조회 시도. memberId={}", memberId);
            throw new NoSuchElementException("해당 memberId의 프로필이 존재하지 않습니다. memberId=" + memberId);
        }
        return profileId.get();
    }

    @Transactional(readOnly = true)
    public boolean existsByMemberId(UUID memberId) {
        return profileRepository.existsByMemberId(memberId);
    }

    @Transactional
    public Profile createProfile(Profile profile) {
        log.debug("프로필 생성 시도. memberId={}", profile.getMemberId());

        if (this.existsByMemberId(profile.getMemberId())) {
            log.warn("이미 존재하는 memberId의 프로필 생성 시도. memberId={}", profile.getMemberId());
            throw new IllegalStateException("프로필이 이미 존재합니다. memberId=" + profile.getMemberId());
        }
        Profile savedProfile = profileRepository.save(profile);
        log.debug("프로필 생성 성공. id={}, memberId={}", savedProfile.getId(), savedProfile.getMemberId());
        return savedProfile;
    }

    @Transactional
    public Profile saveProfile(Profile profile) {
        return profileRepository.save(profile);
    }

    @Transactional
    public void deleteById(UUID profileId) {
        log.debug("프로필 삭제 시도. id={}", profileId);
        if (!profileRepository.existsById(profileId)) {
            log.warn("존재하지 않는 id의 프로필 삭제 시도. id={}", profileId);
            throw new NoSuchElementException("해당 Id의 프로필이 존재하지 않습니다. id=" + profileId);
        }
        profileRepository.deleteById(profileId);
        log.debug("프로필 삭제 성공. id={}", profileId);
    }
}
