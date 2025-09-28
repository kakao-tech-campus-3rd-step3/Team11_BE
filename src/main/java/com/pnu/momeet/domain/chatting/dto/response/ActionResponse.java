package com.pnu.momeet.domain.chatting.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pnu.momeet.domain.chatting.enums.ChatActionType;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ActionResponse(
    Long participantId,
    UUID profileId,
    String nickname,
    String profileImageUrl,
    ChatActionType action
) {
}
