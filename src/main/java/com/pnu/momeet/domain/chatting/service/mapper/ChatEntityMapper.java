package com.pnu.momeet.domain.chatting.service.mapper;

import com.pnu.momeet.domain.chatting.dto.response.ActionResponse;
import com.pnu.momeet.domain.chatting.dto.response.MessageResponse;
import com.pnu.momeet.domain.chatting.entity.ChatMessage;
import com.pnu.momeet.domain.chatting.enums.ChatActionType;
import com.pnu.momeet.domain.participant.entity.Participant;

public class ChatEntityMapper {

    private ChatEntityMapper() {
        // private constructor to prevent instantiation
    }
    public static MessageResponse toMessage(ChatMessage message) {
        return new MessageResponse(
                message.getType(),
                message.getContent(),
                message.getSender().getId(),
                message.getCreatedAt()
        );
    }

    public static ActionResponse toAction(Participant participant, ChatActionType actionType) {
        return new ActionResponse(
                participant.getId(),
                participant.getProfile().getId(),
                participant.getProfile().getNickname(),
                participant.getProfile().getImageUrl(),
                actionType
        );
    }

    public static ActionResponse toAction(Long participantId, ChatActionType actionType) {
        return new ActionResponse(
                participantId,
                null,
                null,
                null,
                actionType
        );
    }

    public static ActionResponse toAction(ChatActionType actionType) {
        return new ActionResponse(
                null,
                null,
                null,
                null,
                actionType
        );
    }
}
