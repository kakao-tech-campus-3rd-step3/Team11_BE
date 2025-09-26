package com.pnu.momeet.domain.chatting.dto.response;

import com.pnu.momeet.domain.chatting.enums.ChatMessageType;

import java.time.LocalDateTime;

public record MessageResponse(
        ChatMessageType type,
        String content,
        Long senderId,
        LocalDateTime sentAt
) {
}
