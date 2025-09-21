package com.pnu.momeet.domain.meetup.service;

import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.repository.MeetupDslRepository;
import com.pnu.momeet.domain.meetup.repository.MeetupRepository;
import com.pnu.momeet.domain.profile.service.ProfileService;
import com.pnu.momeet.domain.sigungu.service.SigunguEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetupEntityService {
    private final MeetupRepository meetupRepository;
    private final MeetupDslRepository meetupDslRepository;

    private final ProfileService profileService;
    private final SigunguEntityService sigunguService;

    @Transactional(readOnly = true)
    public Meetup getReferenceById(UUID meetupId) {
        return meetupRepository.getReferenceById(meetupId);
    }

    @Transactional(readOnly = true)
    public Meetup getById(UUID meetupId) {
        log.debug("특정 id의 meetup 조회 시도. id={}", meetupId);
        var meetup = meetupRepository.findById(meetupId);
        if (meetup.isEmpty()) {
            log.warn("존재하지 않는 id의 meetup 조회 시도. id={}", meetupId);
            throw new NoSuchElementException("해당 Id의 모임이 존재하지 않습니다. id=" + meetupId);
        }
        log.debug("특정 id의 meetup 조회 성공. id={}", meetupId);
        return meetup.get();
    }

    @Transactional(readOnly = true)
    public boolean existsById(UUID meetupId) {
        return meetupRepository.existsById(meetupId);
    }

    @Transactional
    public Meetup createMeetup(Meetup meetup) {
        log.debug("새로운 meetup 생성 시도. , ownerId={}", meetup.getOwner().getId());
        var createdMeetup = this.saveMeetup(meetup);
        log.debug("새로운 meetup 생성 성공. id={}, ownerId={}", createdMeetup.getId(), createdMeetup.getOwner().getId());
        return createdMeetup;
    }

    @Transactional
    public Meetup createMeetup(Meetup meetup, List<String> hashTags) {
        var createdMeetup = this.createMeetup(meetup);
        createdMeetup.setHashTags(hashTags);
        return createdMeetup;
    }

    @Transactional
    public Meetup saveMeetup(Meetup meetup) {
        return meetupRepository.save(meetup);
    }

    @Transactional
    public Meetup updateMeetup(Meetup meetup, Consumer<Meetup> updater) {
        log.debug("특정 id의 meetup 수정 시도. id={}", meetup.getId());
        updater.accept(meetup);
        log.debug("특정 id의 meetup 수정 성공. id={}", meetup.getId());
        return meetup;
    }

    @Transactional
    public void deleteById(UUID meetupId) {
        log.debug("특정 id의 meetup 삭제 시도. id={}", meetupId);
        if (!this.existsById(meetupId)) {
            log.warn("존재하지 않는 id의 meetup 삭제 시도. id={}", meetupId);
            throw new NoSuchElementException("해당 Id의 모임이 존재하지 않습니다. id=" + meetupId);
        }
        meetupRepository.deleteById(meetupId);
        log.debug("특정 id의 meetup 삭제 성공. id={}", meetupId);
    }

}
