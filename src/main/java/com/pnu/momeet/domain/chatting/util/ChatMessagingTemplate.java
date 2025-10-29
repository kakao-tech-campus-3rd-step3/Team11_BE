package com.pnu.momeet.domain.chatting.util;

import com.pnu.momeet.common.exception.MessageSendFailureException;
import com.pnu.momeet.domain.chatting.dto.response.ActionResponse;
import com.pnu.momeet.domain.chatting.dto.response.MessageResponse;
import com.pnu.momeet.domain.chatting.enums.ChatActionType;
import com.pnu.momeet.domain.chatting.service.mapper.ChatEntityMapper;
import com.pnu.momeet.domain.participant.entity.Participant;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatMessagingTemplate {
    private final SimpMessagingTemplate messagingTemplate;
    private final static String TOPIC_PREFIX = "/topic/meetups/";

    @Transactional
    public void sendMessage(UUID meetupId, MessageResponse response) {
        try {
            messagingTemplate.convertAndSend(TOPIC_PREFIX + meetupId + "/messages", response);
            // 액션 상태 브로드캐스트
            sendAction(meetupId, response.senderId(), ChatActionType.MESSAGE);
        } catch (Exception e) {
            throw new MessageSendFailureException(e.getMessage());
        }
    }

    @Transactional
    public void sendAction(UUID meetupId, Participant participant, ChatActionType action) {
        ActionResponse actionResponse = ChatEntityMapper.toAction(participant, action);
        try {
            messagingTemplate.convertAndSend(
                    TOPIC_PREFIX + meetupId + "/actions",
                    actionResponse
            );
        } catch (Exception e) {
            throw new MessageSendFailureException(e.getMessage());
        }
    }

    @Transactional
    public void sendAction(UUID meetupId, Long participantId, ChatActionType action) {
        ActionResponse actionResponse = ChatEntityMapper.toAction(participantId, action);
        try {
            messagingTemplate.convertAndSend(
                    TOPIC_PREFIX + meetupId + "/actions",
                    actionResponse
            );
        } catch (Exception e) {
            throw new MessageSendFailureException(e.getMessage());
        }
    }

    @Transactional
    public void sendAction(UUID meetupId, ChatActionType action) {
        ActionResponse actionResponse = ChatEntityMapper.toAction(action);
        try {
            messagingTemplate.convertAndSend(
                    TOPIC_PREFIX + meetupId + "/actions",
                    actionResponse
            );
        } catch (Exception e) {
            throw new MessageSendFailureException(e.getMessage());
        }
    }
}
