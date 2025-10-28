package com.pnu.momeet.domain.participant.service;

import com.pnu.momeet.common.event.CoreEventPublisher;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import com.pnu.momeet.domain.meetup.service.MeetupEntityService;
import com.pnu.momeet.domain.participant.dto.response.ParticipantResponse;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.enums.MeetupRole;
import com.pnu.momeet.domain.participant.event.ParticipantExitEvent;
import com.pnu.momeet.domain.participant.event.ParticipantJoinEvent;
import com.pnu.momeet.domain.participant.event.ParticipantKickEvent;
import com.pnu.momeet.domain.participant.service.mapper.ParticipantDtoMapper;
import com.pnu.momeet.domain.participant.service.mapper.ParticipantEntityMapper;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantDomainService {
    private final ParticipantEntityService entityService;
    private final MeetupEntityService meetupService;
    private final ProfileEntityService profileService;
    private final CoreEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<ParticipantResponse> getParticipantsByMeetupId(UUID meetupId) {
        if (!meetupService.existsById(meetupId)) {
            log.info("존재하지 않는 모임 ID로 참가자 조회 시도. meetupId={}", meetupId);
            throw new NoSuchElementException("해당 모임이 존재하지 않습니다.");
        }
        return entityService.getAllByMeetupId(meetupId)
                .stream()
                .map(ParticipantEntityMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponse> getParticipantsVisibleToViewer(
        UUID meetupId,
        UUID viewerMemberId
    ) {
        if (!profileService.existsByMemberId(viewerMemberId)) {
            log.info("존재하지 않는 멤버 ID로 참가자 조회 시도. memberId={}", viewerMemberId);
            throw new NoSuchElementException("해당 멤버가 존재하지 않습니다.");
        }

        if (!meetupService.existsById(meetupId)) {
            log.info("존재하지 않는 모임 ID로 참가자 조회 시도. meetupId={}", meetupId);
            throw new NoSuchElementException("해당 모임이 존재하지 않습니다.");
        }
        return entityService.findAllVisibleByMeetupId(meetupId, viewerMemberId)
            .stream()
            .map(ParticipantEntityMapper::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public ParticipantResponse getMyParticipantInfo(UUID meetupId, UUID memberId) {
        Profile profile = profileService.getByMemberId(memberId);
        Participant participant = entityService.getByProfileIDAndMeetupID(profile.getId(), meetupId);
        return ParticipantEntityMapper.toDto(participant);
    }

    @Transactional
    public ParticipantResponse joinMeetup(UUID meetupId, UUID memberId) {
        return joinMeetup(meetupId, memberId, MeetupRole.MEMBER);
    }
    @Transactional
    public ParticipantResponse joinMeetup(UUID meetupId, UUID memberId, MeetupRole role) {
        Profile profile = profileService.getByMemberId(memberId);
        if (entityService.existsByProfileIdAndMeetupId(profile.getId(), meetupId)) {
            log.info("이미 참여한 모임에 다시 참여 시도. meetupId={}, profileId={}", meetupId, profile.getId());
            throw new IllegalStateException("이미 참여한 모임입니다.");
        }
        Meetup meetup = meetupService.getById(meetupId);
        if (meetup.getStatus() != MeetupStatus.OPEN) {
            log.info("모임에 참여할 수 없는 상태에서 참여 시도. meetupId={}, profileId={}, status={}",
                    meetupId, profile.getId(), meetup.getStatus());

            throw new IllegalArgumentException("모임에 참여할 수 없는 상태입니다. 현재 상태: "
                    + meetup.getStatus().getDescription());
        }

        if (meetup.getParticipantCount() >= meetup.getCapacity()) {
            log.info("모임 정원 초과로 인한 참여 시도 실패. meetupId={}, profileId={}", meetupId, profile.getId());
            throw new IllegalArgumentException("모임 정원이 초과되었습니다.");
        }
        Participant createdParticipant = entityService.createParticipant(
            ParticipantDtoMapper.toEntity(profile, meetup, role)
        );
        // 참가자 추가 및 참가자 수 증가
        meetupService.updateMeetup(meetup, m -> m.addParticipant(createdParticipant));
        log.info("모임 참가 성공. meetupId={}, profileId={}", meetupId, profile.getId());

        eventPublisher.publish(new ParticipantJoinEvent(meetupId, createdParticipant.getId()));
        return ParticipantEntityMapper.toDto(createdParticipant);
    }

    @Transactional
    public void leaveMeetup(UUID meetupId, UUID memberId) {
        Profile profile = profileService.getByMemberId(memberId);
        Meetup meetup = meetupService.getById(meetupId);
        Participant participant = entityService.getByProfileIDAndMeetupID(profile.getId(), meetupId);
        
        if (meetup.getStatus() != MeetupStatus.OPEN) {
            log.info("모임에서 나갈 수 없는 상태에서 나감 시도. meetupId={}, profileId={}, status={}",
                    meetupId, profile.getId(), meetup.getStatus());
            throw new IllegalArgumentException("모임에서 나갈 수 없는 상태입니다. 현재 상태: "
                    + meetup.getStatus().getDescription());
        }
        Pair<Participant, Participant> topTwoParticipants;
        try {
             topTwoParticipants = entityService.getTopTwoByTemperatureDesc(meetupId);
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("참가자가 2명 미만인 모임에서는 호스트가 나갈 수 없습니다.");
        }
        if (participant.getRole() == MeetupRole.HOST) {
            log.info("호스트가 모임에서 나감 시도. meetupId={}, profileId={}", meetupId, profile.getId());
            Participant replacementHost = topTwoParticipants.getFirst().getId().equals(participant.getId())
                    ? topTwoParticipants.getSecond() // 호스트가 1등일 경우 2등이 호스트 됨
                    : topTwoParticipants.getFirst(); // 아닐 경우 온도가 가장 높은 참가자가 호스트 됨

            entityService.updateParticipant(replacementHost, p -> p.setRole(MeetupRole.HOST));
            meetupService.updateMeetup(meetup, m -> m.setOwner(replacementHost.getProfile()));
        }
        // 채팅방 퇴장 알림
        eventPublisher.publish(new ParticipantExitEvent(meetupId, participant.getId()));
        // 참가자 제거 및 참가자 수 감소
        meetupService.updateMeetup(meetup, m -> m.removeParticipant(participant));

        log.info("모임 탈퇴 성공. meetupId={}, profileId={}", meetupId, profile.getId());
    }

    @Transactional
    public void kickParticipant(UUID meetupId, UUID memberId, Long targetParticipantId) {
        UUID profileId = profileService.mapToProfileId(memberId);
        Participant requester = entityService.getByProfileIDAndMeetupID(profileId, meetupId);
        if (requester.getRole() != MeetupRole.HOST) {
            log.info("호스트가 아닌 참가자가 강퇴 시도. requesterId={}, targetParticipantId={}",
                    requester.getId(), targetParticipantId);
            throw new SecurityException("호스트만 참가자를 강퇴할 수 있습니다.");
        }
        if (requester.getId().equals(targetParticipantId)) {
            log.info("스스로를 강퇴 시도. requesterId={}", requester.getId());
            throw new IllegalArgumentException("스스로를 강퇴할 수 없습니다.");
        }
        Participant targetParticipant = entityService.getByIdAndMeetupId(targetParticipantId, meetupId);

        entityService.updateParticipant(requester, p -> p.setLastActiveAt(LocalDateTime.now()));
        // 채팅방 강퇴 알림
        eventPublisher.publish(new ParticipantKickEvent(meetupId, targetParticipant.getId()));
        // 참가자 제거 및 참가자 수 감소
        meetupService.updateMeetup(meetupService.getById(meetupId), m -> m.removeParticipant(targetParticipant));

        log.info("참가자 강퇴 성공. byId={}, targetParticipantId={}", requester.getId(), targetParticipantId);
    }

    @Transactional
    public ParticipantResponse grantHostRole(UUID meetupId, UUID memberId, Long targetParticipantId) {
        UUID profileId = profileService.mapToProfileId(memberId);
        Participant requester = entityService.getByProfileIDAndMeetupID(profileId, meetupId);
        if (requester.getRole() != MeetupRole.HOST) {
            throw new SecurityException("호스트만 호스트 권한을 양도할 수 있습니다.");
        }
        if (requester.getId().equals(targetParticipantId)) {
            throw new IllegalStateException("스스로에게 호스트 권한을 양도할 수 없습니다.");
        }
        Participant targetParticipant = entityService.getByIdAndMeetupId(targetParticipantId, meetupId);
        // 기존 호스트를 MEMBER로 변경
        entityService.updateParticipant(requester, p -> {
            p.setRole(MeetupRole.MEMBER);
            p.setLastActiveAt(LocalDateTime.now());
        });
        // 새로운 호스트를 HOST로 변경
        entityService.updateParticipant(targetParticipant, p -> p.setRole(MeetupRole.HOST));
        // 모임의 owner도 변경
        meetupService.updateMeetup(requester.getMeetup(), m -> m.setOwner(targetParticipant.getProfile()));
        log.info("호스트 권한 양도 성공. fromId={}, toId={}", requester.getId(), targetParticipant.getId());
        return ParticipantEntityMapper.toDto(targetParticipant);
    }
}
