package com.pnu.momeet.domain.chatting.service.mapper;

import com.pnu.momeet.domain.chatting.dto.response.MessageResponse;
import com.pnu.momeet.domain.chatting.entity.ChatMessage;

public class ChatEntityMapper {

    private ChatEntityMapper() {
        // private constructor to prevent instantiation
    }
    public static MessageResponse toMessage(ChatMessage message) {
        return new MessageResponse(
                message.getType(),
                message.getContent(),
                message.getSender().getId(),
                message.getSender().getProfile().getId(),
                message.getSender().getProfile().getNickname(),
                message.getSender().getProfile().getImageUrl(),
                message.getCreatedAt()
        );
    }
}
