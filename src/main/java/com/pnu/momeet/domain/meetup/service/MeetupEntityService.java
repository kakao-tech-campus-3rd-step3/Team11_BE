package com.pnu.momeet.domain.meetup.service;

import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.enums.MainCategory;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import com.pnu.momeet.domain.meetup.enums.SubCategory;
import com.pnu.momeet.domain.meetup.repository.MeetupDslRepository;
import com.pnu.momeet.domain.meetup.repository.MeetupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetupEntityService {
    private final MeetupRepository meetupRepository;
    private final MeetupDslRepository meetupDslRepository;

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
    public Meetup getByIdWithDetails(UUID meetupId) {
        log.debug("특정 id의 meetup 상세 조회 시도. id={}", meetupId);
        var meetup = meetupDslRepository.findByIdWithDetails(meetupId);
        if (meetup.isEmpty()) {
            log.warn("존재하지 않는 id의 meetup 상세 조회 시도. id={}", meetupId);
            throw new NoSuchElementException("해당 Id의 모임이 존재하지 않습니다. id=" + meetupId);
        }
        log.debug("특정 id의 meetup 상세 조회 성공. id={}", meetupId);
        return meetup.get();
    }

    @Transactional(readOnly = true)
    public boolean existsById(UUID meetupId) {
        return meetupRepository.existsById(meetupId);
    }

    @Transactional(readOnly = true)
    public Page<Meetup> getAllBySpecificationWithPagination(
            Specification<Meetup> spec,
            Pageable pageable
    ) {
        log.debug("특정 조건의 meetup 목록 조회 시도.");
        var pageResult = meetupRepository.findAll(spec, pageable);
        log.debug("특정 조건의 meetup 목록 조회 성공. size={}", pageResult.getNumberOfElements());
        return pageResult;
    }

    @Transactional(readOnly = true)
    public List<Meetup> getAllByLocationAndCondition(
            Point location,
            Double radius,
            Optional<MainCategory> mainCategory,
            Optional<SubCategory> subCategory,
            Optional<String> search
    ) {
        log.debug("특정 위치 기반 meetup 목록 조회 시도. location={}, radius={}", location, radius);
        var meetups = meetupDslRepository.findAllByDistanceAndPredicates(
                location, radius, mainCategory, subCategory, search
        );
        log.debug("특정 위치 기반 meetup 목록 조회 성공. size={}", meetups.size());
        return meetups;
    }

    @Transactional(readOnly = true)
    public List<Meetup> getAllByOwnerIdAndStatusIn(UUID profileId, List<MeetupStatus> statuses) {
        log.debug("특정 회원이 소유한 특정 상태의 meetup 목록 조회 시도. ownerId={}, statuses={}", profileId, statuses);
        var meetups = meetupDslRepository.findAllByOwnerIdAndStatusIn(profileId, statuses);
        log.debug("특정 회원이 소유한 특정 상태의 meetup 목록 조회 성공. ownerId={}, size={}", profileId, meetups.size());
        return meetups;
    }

    @Transactional(readOnly = true)
    public List<Meetup> getAllByStatusAndEndAtBefore(MeetupStatus status, LocalDateTime before) {
        log.debug("특정 상태이면서 특정 시간 이전에 종료된 meetup 목록 조회 시도. status={}, before={}", status, before);
        var meetups = meetupRepository.findAllByStatusAndEndAtBefore(status, before);
        log.debug("특정 상태이면서 특정 시간 이전에 종료된 meetup 목록 조회 성공. status={}, size={}", status, meetups.size());
        return meetups;
    }

    @Transactional(readOnly = true)
    public boolean existsByOwnerIdAndStatusIn(UUID profileId, List<MeetupStatus> statuses) {
        return meetupDslRepository.existsByOwnerIdAndStatusIn(profileId, statuses);
    }

    @Transactional(readOnly = true)
    public Page<Meetup> getEndedMeetupsByProfileId(UUID profileId, Pageable pageable) {
        log.debug("특정 회원이 소유한 종료된 meetup 목록 조회 시도. ownerId={}", profileId);
        var pageResult = meetupDslRepository.findEndedMeetupsByProfileId(profileId, pageable);
        log.debug("특정 회원이 소유한 종료된 meetup 목록 조회 성공. ownerId={}, size={}", profileId, pageResult.getNumberOfElements());
        return pageResult;
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
        log.debug("새로운 meetup 해시태그 설정 성공. id={}, size={}", createdMeetup.getId(), hashTags.size());
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
