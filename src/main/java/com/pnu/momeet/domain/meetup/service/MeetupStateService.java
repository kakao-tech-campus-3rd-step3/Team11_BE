package com.pnu.momeet.domain.meetup.service;

import com.pnu.momeet.common.event.CoreEventPublisher;
import com.pnu.momeet.domain.evaluation.event.EvaluationDeadlineEndedEvent;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupEntityMapper;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.service.ParticipantEntityService;
import com.pnu.momeet.domain.participant.service.BanParticipantService;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetupStateService {
    private final MeetupEntityService entityService;
    private final ProfileEntityService profileService;
    private final ParticipantEntityService participantService;
    private final MeetupEntityService meetupEntityService;
    private final BanParticipantService userBanService;
    private final CoreEventPublisher coreEventPublisher;


    @Transactional
    public void startMeetupByMemberId(UUID memberId) {
        // 1) 소유자 검증: memberId -> profileId -> 소유한 OPEN 모임 중 해당 id 확인
        UUID ownerProfileId = profileService.mapToProfileId(memberId);
        var meetups = entityService.getAllByOwnerIdAndStatusIn(ownerProfileId, List.of(MeetupStatus.OPEN));
        if (meetups.isEmpty()) {
            throw new NoSuchElementException("시작 가능한 모임이 없습니다.");
        }
        startMeetupInternal(meetups.getFirst());
    }

    @Transactional
    public void startMeetupById(UUID meetupId) {
        Meetup meetup = entityService.getById(meetupId);
        if (meetup.getStatus() != MeetupStatus.OPEN) {
            throw new IllegalStateException("시작할 수 있는 상태가 아닙니다.");
        }
        startMeetupInternal(meetup);
    }

    private void startMeetupInternal(Meetup meetup) {
        entityService.updateMeetup(meetup, m -> m.setStatus(MeetupStatus.IN_PROGRESS));
        coreEventPublisher.publish(MeetupEntityMapper.toMeetupStartEvent(meetup));
        log.info("모임 시작 완료. id={}, ownerId={}", meetup.getId(), meetup.getOwner().getId());
    }

    @Transactional
    public void cancelMeetupById(UUID meetupId) {
        Meetup meetup = entityService.getById(meetupId);
        if (meetup.getStatus() != MeetupStatus.OPEN && meetup.getStatus() != MeetupStatus.IN_PROGRESS) {
            throw new IllegalStateException("모임을 취소할 수 있는 상태가 아닙니다.");
        }
        cancelMeetupInternal(meetup, Role.ROLE_ADMIN);
    }

    @Transactional
    public void cancelMeetupMemberId(UUID memberId) {
        UUID profileId = profileService.mapToProfileId(memberId);
        var meetups = entityService.getAllByOwnerIdAndStatusIn(profileId, List.of(MeetupStatus.OPEN));
        if (meetups.isEmpty()) {
            throw new NoSuchElementException("취소 가능한 모임이 없습니다.");
        }
        cancelMeetupInternal(meetups.getFirst(), Role.ROLE_USER);
    }

    private void cancelMeetupInternal(Meetup meetup, Role canceledBy) {
        entityService.updateMeetup(meetup, m -> m.setStatus(MeetupStatus.CANCELED));
        coreEventPublisher.publish(MeetupEntityMapper.toMeetupCanceledEvent(meetup, canceledBy));
        userBanService.clearBanList(meetup.getId());
        log.info("모임 취소 성공. id={}, canceledBy={}", meetup.getId(), canceledBy);
    }

    @Transactional
    public void finishMeetupById(UUID meetupId, Role finishedBy) {
        Meetup meetup = entityService.getById(meetupId);
        if (meetup.getStatus() != MeetupStatus.IN_PROGRESS) {
            throw new IllegalStateException("종료할 수 있는 상태가 아닙니다.");
        }
        if (finishedBy != Role.ROLE_SYSTEM && finishedBy != Role.ROLE_ADMIN) {
            // 이 메서드가 다른 역할로 호출되는 것은 비정상적 상황임
            log.warn("비시스템/비관리자에 의한 모임 종료 시도 감지. meetupId={}, finishedBy={}", meetupId, finishedBy);
            throw new IllegalArgumentException("시스템 또는 관리자에 의해서만 모임을 종료할 수 있습니다.");
        }
        finishMeetupInternal(meetup, finishedBy);
    }

    @Transactional
    public void finishMeetupByMemberId(UUID memberId) {
        // 1) 소유자 검증: memberId -> profileId -> 소유한 IN_PROGRESS 모임 중 해당 id 확인
        UUID ownerProfileId = profileService.mapToProfileId(memberId);
        List<Meetup> availableMeetups = entityService.getAllByOwnerIdAndStatusIn(
                ownerProfileId, List.of(MeetupStatus.IN_PROGRESS)
        );
        if (availableMeetups.isEmpty()) {
            throw new IllegalStateException("종료할 수 있는 모임이 없습니다.");
        }
        finishMeetupInternal(availableMeetups.getFirst(), Role.ROLE_USER);
    }

    private void finishMeetupInternal(Meetup meetup, Role finishedBy) {
        // 2) 상태 전이: ENDED (엔티티 메서드로 캡슐화 권장)
        entityService.updateMeetup(meetup, m -> m.setStatus(MeetupStatus.ENDED));

        // 3) 완주자 조회 -> 프로필 집계 필드 증가
        var participants = participantService.getAllByMeetupId(meetup.getId());
        participants.stream()
                .map(Participant::getProfile)
                .forEach(Profile::increaseCompletedJoinMeetups);

        // 4) 종료 이벤트 발행 (커밋 후 배지 부여)
        coreEventPublisher.publish(MeetupEntityMapper.toMeetupFinishedEvent(meetup, participants, finishedBy));

        log.info("모임 종료 완료. meetupId={}, ownerProfileId={}, finisherCount={}",
                meetup.getId(), meetup.getOwner().getId(), participants.size());
        userBanService.clearBanList(meetup.getId());
    }

    @Transactional
    public void evaluationPeriodEnded(Meetup meetup) {
        if (meetup.getStatus() != MeetupStatus.ENDED) {
            // 시스템에 의해 관리되는 상태 전이이므로, 관리자 개입 필요
            log.warn("평가 기간 종료 처리 중 상태 이상 감지. id={}, status={}", meetup.getId(), meetup.getStatus());
            throw new IllegalStateException("평가 기간 종료 처리를 할 수 없는 상태입니다.");
        }
        meetupEntityService.updateMeetup(meetup, m -> m.setStatus(MeetupStatus.EVALUATION_TIMEOUT));
        coreEventPublisher.publish(new EvaluationDeadlineEndedEvent(meetup.getId()));
        log.info("평가 기간 종료에 따른 모임 상태 변경 완료. id={}, status={}", meetup.getId(), meetup.getStatus());
    }
}
