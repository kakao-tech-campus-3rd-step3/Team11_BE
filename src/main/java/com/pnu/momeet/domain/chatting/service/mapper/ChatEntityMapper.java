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
        Long senderId = message.getSender() != null ? message.getSender().getId() : null;

        if (message.getProfile() == null) {
            return new MessageResponse(
                    message.getType(),
                    message.getContent(),
                    senderId,
                    null,
                    null,
                    null,
                    message.getCreatedAt()
            );
        }

        return new MessageResponse(
                message.getType(),
                message.getContent(),
                senderId,
                message.getProfile().getId(),
                message.getProfile().getNickname(),
                message.getProfile().getImageUrl(),
                message.getCreatedAt()
        );
    }

    public static ActionResponse toAction(Participant participant, ChatActionType actionType) {
        if (participant.getProfile() == null) {
            return toAction(participant.getId(), actionType);
        }

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
