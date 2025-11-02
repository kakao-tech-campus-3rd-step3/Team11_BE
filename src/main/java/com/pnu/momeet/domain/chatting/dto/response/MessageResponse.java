package com.pnu.momeet.domain.chatting.dto.response;

import com.pnu.momeet.domain.chatting.enums.ChatMessageType;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        ChatMessageType type,
        String content,
        Long senderId,
        UUID profileId,
        String nickname,
        String profileImageUrl,
        LocalDateTime sentAt
) {
}
