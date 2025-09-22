package com.pnu.momeet.domain.chatting.service;

import com.pnu.momeet.common.exception.MessageSendFailureException;
import com.pnu.momeet.domain.chatting.dto.request.MessageRequest;
import com.pnu.momeet.domain.chatting.dto.response.ActionResponse;
import com.pnu.momeet.domain.chatting.dto.response.MessageResponse;
import com.pnu.momeet.domain.chatting.enums.ChatActionType;
import com.pnu.momeet.domain.chatting.enums.ChatMessageType;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.service.ParticipantDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChattingService {

    private final static String TOPIC_PREFIX = "/topic/meetups/";
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;
    private final ParticipantDomainService participantService;

    @Transactional
    public void sendMessage(UUID meetupId, UUID memberId, MessageRequest message) {
        // 입력 검증
        validateMessageRequest(message);
        // 메시지 저장
        MessageResponse response = chatMessageService.saveMessage(meetupId, memberId, message);

        // 메시지 브로드캐스트
        try {
            messagingTemplate.convertAndSend(TOPIC_PREFIX + meetupId + "/messages", response);
            // 액션 상태 브로드캐스트
            messagingTemplate.convertAndSend(TOPIC_PREFIX + meetupId + "/actions",
                    new ActionResponse(response.senderId(), ChatActionType.MESSAGE)
            );
        } catch (Exception e) {
            throw new MessageSendFailureException(e.getMessage());
        }
    }

    @Transactional
    public void connectToMeetup(UUID meetupId, UUID memberId) {
        Participant participant = participantService.getEntityByMemberIdAndMeetupId(memberId, meetupId);
        if (!participant.getIsActive()) {
            participant.setLastActiveAt(LocalDateTime.now());
            participant.setIsActive(true);
            sendAction(meetupId, memberId, ChatActionType.ENTER);
            log.debug("사용자 채팅방 입장 - meetupId: {}, memberId: {}", meetupId, memberId);
        }
    }

    @Transactional
    public void disconnectFromMeetup(UUID meetupId, UUID memberId) {
        Participant participant = participantService.getEntityByMemberIdAndMeetupId(memberId, meetupId);
        if (participant.getIsActive()) {
            participant.setIsActive(false);
            sendAction(meetupId, memberId, ChatActionType.LEAVE);

            log.debug("사용자 채팅방 퇴장 - meetupId: {}, memberId: {}", meetupId, memberId);
        }
    }

    @Transactional
    public void disconnectAllFromMeetup(UUID memberId) {
        participantService.getJoiningParticipantsByMemberId(memberId)
            .stream()
            .filter(Participant::getIsActive)
            .forEach(participant -> {
               participant.setIsActive(false);
                sendAction(participant.getMeetup().getId(), memberId, ChatActionType.LEAVE);
            });

        log.debug("사용자 모든 채팅방 퇴장 - memberId: {}", memberId);
    }

    private void sendAction(UUID meetupId, UUID memberId, ChatActionType actionType) {
        Participant participant = participantService.getEntityByMemberIdAndMeetupId(memberId, meetupId);
        ActionResponse actionResponse = new ActionResponse(participant.getId(), actionType);
        try {
            messagingTemplate.convertAndSend(
                    TOPIC_PREFIX + meetupId + "/actions",
                    actionResponse
            );
        } catch (Exception e) {
            throw new MessageSendFailureException(e.getMessage());
        }
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
}
