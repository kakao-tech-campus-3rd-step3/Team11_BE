package com.pnu.momeet.domain.chatting.service;

import com.pnu.momeet.domain.chatting.util.ChatMessagingTemplate;
import com.pnu.momeet.domain.chatting.dto.request.MessageRequest;
import com.pnu.momeet.domain.chatting.dto.response.MessageResponse;
import com.pnu.momeet.domain.chatting.enums.ChatActionType;
import com.pnu.momeet.domain.chatting.enums.ChatMessageType;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.service.ParticipantEntityService;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChattingService {

    private final ChatMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;
    private final ParticipantEntityService participantService;
    private final ProfileEntityService profileService;

    @Transactional
    public void sendMessage(UUID meetupId, UUID memberId, MessageRequest message) {
        // 입력 검증
        validateMessageRequest(message);
        // 메시지 저장
        MessageResponse response = chatMessageService.saveMessage(meetupId, memberId, message);
        // 메시지 브로드캐스트
        messagingTemplate.sendMessage(meetupId, response);
        log.debug("사용자 메시지 전송 - meetupId: {}, memberId: {}, content: {}",meetupId, memberId, message.content());
    }

    private void validateMessageRequest(MessageRequest message) {
        if (message == null || !StringUtils.hasText(message.content())) {
            throw new IllegalArgumentException("메시지 내용이 비어 있습니다.");
        }
        if (message.content().length() > 1000) {
            throw new IllegalArgumentException("메시지 내용이 너무 깁니다. 최대 1000자까지 허용됩니다.");
        }
        try {
            ChatMessageType.valueOf(message.type());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 메시지 타입입니다: " + message.type());
        }
    }

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
            log.debug("사용자 채팅방 입장 - meetupId: {}, memberId: {}", meetupId, memberId);
        }
    }

    @Transactional
    public void disconnectFromMeetup(UUID meetupId, UUID memberId) {
        UUID profileId = profileService.mapToProfileId(memberId);
        Participant participant = participantService.getByProfileIDAndMeetupID(profileId, meetupId);

        if (participant.getIsActive()) {
            participantService.updateParticipant(participant, p -> p.setIsActive(false));
            messagingTemplate.sendAction(meetupId, participant.getId(), ChatActionType.LEAVE);
            log.debug("사용자 채팅방 연결 종료 - meetupId: {}, memberId: {}", meetupId, memberId);
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
                    log.debug("사용자 다건 채팅방 연결 종료 - meetupId: {}, memberId: {}", participant.getMeetup().getId(), memberId);
                });
    }
}
