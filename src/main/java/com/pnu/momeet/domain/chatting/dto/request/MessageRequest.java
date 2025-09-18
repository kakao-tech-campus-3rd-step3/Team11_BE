package com.pnu.momeet.domain.chatting.dto.request;

public record MessageRequest(
        String type,
        String content
) {
}
