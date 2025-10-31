package com.pnu.momeet.domain.chatting.service;

import com.pnu.momeet.domain.chatting.entity.ChatMessage;
import com.pnu.momeet.domain.chatting.enums.ChatMessageType;
import com.pnu.momeet.domain.chatting.repository.ChatMessageRepository;
import com.pnu.momeet.domain.common.dto.response.CursorInfo;
import com.pnu.momeet.domain.participant.entity.Participant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageEntityService {
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public CursorInfo<ChatMessage> getHistories(UUID meetupId, int size, Long cursorId) {
        log.debug("채팅 메시지 히스토리 조회 시도 - meetupId: {}, size: {}, cursorId: {}",
            meetupId, size, cursorId
        );
        var histories = chatMessageRepository.findHistoriesByMeetupId(meetupId, size, cursorId);
        log.debug("채팅 메시지 히스토리 조회 완료 - meetupId: {}, size: {}, cursorId: {}, resultSize: {}",
                meetupId, size, cursorId, histories.getSize());
        return histories;
    }

    @Transactional
    public ChatMessage createMessage(Participant participant, ChatMessageType type, String content) {
        log.debug("채팅 메시지 생성 시도 - meetupId: {}, profileId:{}, type: {}, content:{}",
                participant.getMeetup().getId(),
                participant.getProfile().getId(),
                type,
                content.substring(0, Math.min(content.length(), 20))
        );
        ChatMessage chatMessage = new ChatMessage(participant, type, content);
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        log.debug("채팅 메시지 생성 완료 - messageId: {}, meetupId: {}",
                savedMessage.getId(),
                participant.getMeetup().getId()
        );
        return savedMessage;
    }

    @Transactional
    public void deleteAllByMeetupId(UUID meetupId) {
        log.debug("모임 ID로 모든 채팅 메시지 삭제 시도 - meetupId: {}", meetupId);
        chatMessageRepository.deleteAllByMeetupId(meetupId);
        log.debug("모임 ID로 모든 채팅 메시지 삭제 완료 - meetupId: {}", meetupId);
    }
}
