package com.pnu.momeet.domain.chatting.service;

import com.pnu.momeet.domain.chatting.dto.request.MessageRequest;
import com.pnu.momeet.domain.chatting.dto.response.MessageResponse;
import com.pnu.momeet.domain.chatting.entity.ChatMessage;
import com.pnu.momeet.domain.chatting.enums.ChatMessageType;
import com.pnu.momeet.domain.chatting.service.mapper.ChatEntityMapper;
import com.pnu.momeet.domain.chatting.util.ChatMessagingTemplate;
import com.pnu.momeet.domain.common.dto.response.CursorInfo;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.service.ParticipantEntityService;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ParticipantEntityService participantService;
    private final ChatMessageEntityService entityService;
    private final ProfileEntityService profileService;
    private final ChatMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public CursorInfo<MessageResponse> getHistories(UUID meetupId, UUID memberId, int size, Long cursorId) {
        // 모임 및 참가자 존재 여부 확인
        UUID profileId = profileService.getByMemberId(memberId).getId();
        if (!participantService.existsByProfileIdAndMeetupId(profileId, meetupId)) {
            throw new NoSuchElementException("해당 모임의 참가자가 아닙니다.");
        }
        CursorInfo<ChatMessage> histories = entityService.getHistories(meetupId, memberId, size, cursorId);
        return CursorInfo.convert(histories, ChatEntityMapper::toMessage);
    }

    @Transactional
    public void sendMessage(UUID meetupId, UUID memberId, MessageRequest message) {
        // 입력 검증
        validateMessageRequest(message);
        UUID profileId = profileService.mapToProfileId(memberId);
        Participant participant = participantService.getByProfileIDAndMeetupID(profileId, meetupId);

        if (!participant.getIsActive()) {
            throw new IllegalStateException("채팅방에 입장한 사용자만 메시지를 보낼 수 있습니다.");
        }
        // 메시지 저장
        MessageResponse response = saveMessage(participant, message);
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

    private MessageResponse saveMessage(Participant participant, MessageRequest message) {
        ChatMessageType type = ChatMessageType.valueOf(message.type());
        ChatMessage savedMessage = entityService.createMessage(participant, type, message.content());
        participantService.updateParticipant(participant, p -> p.setLastActiveAt(LocalDateTime.now()));

        return ChatEntityMapper.toMessage(savedMessage);
    }
}
