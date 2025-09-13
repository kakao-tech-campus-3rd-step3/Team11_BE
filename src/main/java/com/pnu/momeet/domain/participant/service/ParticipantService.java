package com.pnu.momeet.domain.participant.service;

import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import com.pnu.momeet.domain.meetup.service.MeetupService;
import com.pnu.momeet.domain.participant.dto.response.ParticipantResponse;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.enums.MeetupRole;
import com.pnu.momeet.domain.participant.repository.ParticipantRepository;
import com.pnu.momeet.domain.participant.service.mapper.ParticipantEntityMapper;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParticipantService {
    private final ParticipantRepository participantRepository;
    private final MeetupService meetupService;
    private final ProfileService profileService;

    @Transactional(readOnly = true)
    public List<ParticipantResponse> getParticipantsByMeetupId(UUID meetupId) {
        if (!meetupService.existsById(meetupId)) {
            throw new NoSuchElementException("해당 모임이 존재하지 않습니다.");
        }
        return participantRepository.findAllByMeetupId(meetupId)
                .stream()
                .map(ParticipantEntityMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Participant getEntityByProfileIDAndMeetupID(UUID profileId, UUID meetupId) {
        return participantRepository.findByProfileIdAndMeetupId(profileId, meetupId)
                .orElseThrow(() -> new NoSuchElementException("참가자를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Participant getEntityByMemberIdAndMeetupId(UUID memberId, UUID meetupId) {
        return participantRepository.findByMemberIdAndMeetupId(memberId, meetupId)
                .orElseThrow(() -> new NoSuchElementException("참가자를 찾을 수 없습니다."));
    }

    @Transactional
    public ParticipantResponse joinMeetup(UUID meetupId, UUID memberId, MeetupRole role) {
        Profile profile = profileService.getProfileEntityByMemberId(memberId);

        if (participantRepository.existsByMeetupIdAndProfileId(meetupId, profile.getId())) {
            throw new IllegalStateException("이미 참여한 모임입니다.");
        }

        Meetup meetup = meetupService.findEntityById(meetupId);

        if (meetup.getStatus() != MeetupStatus.OPEN) {
            throw new IllegalArgumentException("모임에 참여할 수 없는 상태입니다. 현재 상태: "
                    + meetup.getStatus().getDescription());
        }
        if (meetup.getParticipantCount() >= meetup.getCapacity()) {
            throw new IllegalArgumentException("모임 정원이 초과되었습니다.");
        }

        Participant createdParticipant = Participant.builder()
                .meetup(meetup)
                .role(role)
                .profile(profile)
                .isActive(false)
                .isRated(false)
                .lastActiveAt(LocalDateTime.now())
                .build();

        meetup.addParticipant(createdParticipant); // 양방향 연관관계 설정
        meetup.setParticipantCount(meetup.getParticipantCount() + 1);

        return ParticipantEntityMapper.toDto(
                participantRepository.save(createdParticipant)
        );
    }

    @Transactional
    public ParticipantResponse joinMeetup(UUID meetupId, UUID memberId) {
        return joinMeetup(meetupId, memberId, MeetupRole.MEMBER);
    }

    @Transactional
    public void leaveMeetup(UUID meetupId, UUID memberId) {
        Profile profile = profileService.getProfileEntityByMemberId(memberId);
        Meetup meetup = meetupService.findEntityById(meetupId);

        Participant participant = getEntityByProfileIDAndMeetupID(profile.getId(), meetupId);
        if (meetup.getStatus() != MeetupStatus.OPEN) {
            throw new IllegalArgumentException("모임에서 나갈 수 없는 상태입니다. 현재 상태: "
                    + meetup.getStatus().getDescription());
        }

        List<Participant> participants = participantRepository
                .findTopTwoByOrderByTemperatureDesc(meetupId);

        if (participants.size() <= 1) {
            throw new IllegalArgumentException("참가자가 1명 이하인 모임에서 나갈 수 없습니다. 모임을 삭제하세요.");
        }

        // 호스트가 나갈 경우, 두 번째로 점수가 높은 참가자가 호스트가 됨
        if (participants.get(0).getId().equals(participant.getId())
                && participant.getRole() == MeetupRole.HOST) {
            Participant newHost = participants.get(1);
            newHost.setRole(MeetupRole.HOST);
            meetup.setOwner(participants.get(0).getProfile()); // 모임의 owner도 변경
        }

        meetup.setParticipantCount(meetup.getParticipantCount() - 1);
        meetup.removeParticipant(participant); // 양방향 연관관계 설정 해제

        participantRepository.deleteById(participant.getId());
    }

    @Transactional
    public void kickParticipant(UUID meetupId, UUID memberId, Long targetParticipantId) {
        Participant requester = getEntityByMemberIdAndMeetupId(memberId, meetupId);
        if (requester.getRole() != MeetupRole.HOST) {
            throw new SecurityException("호스트만 참가자를 강퇴할 수 있습니다.");
        }
        if (requester.getId().equals(targetParticipantId)) {
            throw new IllegalArgumentException("스스로를 강퇴할 수 없습니다.");
        }
        if (!participantRepository.existsByIdAndMeetupId(targetParticipantId, meetupId)) {
            throw new NoSuchElementException("해당 참가자가 모임에 존재하지 않습니다.");
        }
        updateLastActiveAt(meetupId, memberId);
        participantRepository.deleteById(targetParticipantId);
    }

    @Transactional
    public ParticipantResponse grantHostRole(UUID meetupId, UUID memberId, Long targetParticipantId) {
        Participant requester = getEntityByMemberIdAndMeetupId(memberId, meetupId);
        if (requester.getRole() != MeetupRole.HOST) {
            throw new SecurityException("호스트만 호스트 권한을 양도할 수 있습니다.");
        }
        if (requester.getId().equals(targetParticipantId)) {
            throw new IllegalStateException("스스로에게 호스트 권한을 양도할 수 없습니다.");
        }
        Participant targetParticipant = participantRepository.findByIdAndMeetupId(targetParticipantId, meetupId)
                .orElseThrow(() -> new NoSuchElementException("해당 참가자가 모임에 존재하지 않습니다."));

        // 기존 호스트를 MEMBER로 변경
        requester.setRole(MeetupRole.MEMBER);
        // 새로운 호스트를 HOST로 변경
        targetParticipant.setRole(MeetupRole.HOST);
        // 모임의 owner도 변경
        Meetup meetup = targetParticipant.getMeetup();
        meetup.setOwner(targetParticipant.getProfile());
        // 활동 시간 업데이트
        updateLastActiveAt(meetupId, memberId);

        return ParticipantEntityMapper.toDto(targetParticipant);
    }

    @Transactional
    public void updateLastActiveAt(UUID meetupId, UUID memberId) {
        Participant participant = getEntityByMemberIdAndMeetupId(memberId, meetupId);
        participant.setLastActiveAt(LocalDateTime.now());
    }
}
