package com.pnu.momeet.domain.chatting.dto.response;

import com.pnu.momeet.domain.chatting.enums.ChatActionType;

public record ActionResponse(
    Long participantId,
    ChatActionType action
) {
}
