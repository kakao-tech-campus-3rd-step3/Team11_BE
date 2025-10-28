package com.pnu.momeet.domain.chatting.service;

import com.pnu.momeet.domain.chatting.enums.ChatActionType;
import com.pnu.momeet.domain.chatting.util.ChatMessagingTemplate;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.service.ParticipantEntityService;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatEventService {
    private final ParticipantEntityService participantService;
    private final ProfileEntityService profileService;
    private final ChatMessagingTemplate messagingTemplate;

    @Transactional
    public void connectToMeetup(UUID meetupId, UUID memberId) {
        UUID profileId = profileService.mapToProfileId(memberId);
        Participant participant = participantService.getByProfileIDAndMeetupID(profileId, meetupId);

        if (!participant.getIsActive()) {
            participantService.updateParticipant(participant, p -> {
                p.setLastActiveAt(LocalDateTime.now());
                p.setIsActive(true);
            });
            messagingTemplate.sendAction(meetupId, participant.getId(), ChatActionType.ENTER);
            log.info("사용자 채팅방 입장 - meetupId: {}, memberId: {}", meetupId, memberId);
        }
    }

    @Transactional
    public void disconnectFromMeetup(UUID meetupId, UUID memberId) {
        UUID profileId = profileService.mapToProfileId(memberId);
        Participant participant = participantService.getByProfileIDAndMeetupID(profileId, meetupId);

        if (participant.getIsActive()) {
            participantService.updateParticipant(participant, p -> p.setIsActive(false));
            messagingTemplate.sendAction(meetupId, participant, ChatActionType.LEAVE);
            log.info("사용자 채팅방 연결 종료 - meetupId: {}, memberId: {}", meetupId, memberId);
        }
    }

    @Transactional
    public void disconnectAllFromMeetup(UUID memberId) {
        UUID profileId = profileService.mapToProfileId(memberId);
        participantService.getAllByProfileID(profileId).stream()
                .filter(Participant::getIsActive)
                .forEach(participant -> {
                    participantService.updateParticipant(participant, p -> p.setIsActive(false));
                    messagingTemplate.sendAction(participant.getMeetup().getId(), participant.getId(), ChatActionType.LEAVE);
                    log.info("사용자 다건 채팅방 연결 종료 - meetupId: {}, memberId: {}", participant.getMeetup().getId(), memberId);
                });
    }

    @Transactional(readOnly = true)
    public void notifyNearEndMeetup(UUID meetupId) {
        participantService.getAllByMeetupId(meetupId).stream()
                .filter(Participant::getIsActive)
                .forEach(participant ->
                    messagingTemplate.sendAction(meetupId, participant.getId(), ChatActionType.NEAR_END)
                );
        log.info("모임 종료 임박 알림 전송 완료 - meetupId: {}", meetupId);
    }

    @Transactional(readOnly = true)
    public void finishMeetup(UUID meetupId) {
        participantService.getAllByMeetupId(meetupId).stream()
                .filter(Participant::getIsActive)
                .forEach(participant ->
                    messagingTemplate.sendAction(meetupId, participant.getId(), ChatActionType.END)
                );
        log.info("모임 종료 알림 전송 완료 - meetupId: {}", meetupId);
    }

    @Transactional(readOnly = true)
    public void startMeetup(UUID meetupId) {
        participantService.getAllByMeetupId(meetupId).stream()
                .filter(Participant::getIsActive)
                .forEach(participant ->
                    messagingTemplate.sendAction(meetupId, participant.getId(), ChatActionType.STARTED)
                );
        log.info("모임 시작 알림 전송 완료 - meetupId: {}", meetupId);
    }

    @Transactional(readOnly = true)
    public void cancelMeetup(UUID meetupId) {
        participantService.getAllByMeetupId(meetupId).stream()
                .filter(Participant::getIsActive)
                .forEach(participant ->
                    messagingTemplate.sendAction(meetupId, participant.getId(), ChatActionType.CANCELED)
                );
        log.info("모임 취소 알림 전송 완료 - meetupId: {}", meetupId);
    }

    @Transactional
    public void cancelByAdminMeetup(UUID meetupId) {
        participantService.getAllByMeetupId(meetupId).stream()
                .filter(Participant::getIsActive)
                .forEach(participant ->
                    messagingTemplate.sendAction(meetupId, participant.getId(), ChatActionType.CANCELED_BY_ADMIN)
                );
        log.info("모임 관리자에 의한 모임 취소 알림 전송 완료 - meetupId: {}", meetupId);
    }
}
