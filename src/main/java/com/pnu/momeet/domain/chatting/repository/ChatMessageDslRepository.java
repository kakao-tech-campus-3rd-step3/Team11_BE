package com.pnu.momeet.domain.chatting.repository;

import com.pnu.momeet.domain.chatting.entity.ChatMessage;
import com.pnu.momeet.domain.common.dto.response.CursorInfo;

import java.util.UUID;

public interface ChatMessageDslRepository {
    CursorInfo<ChatMessage> findHistoriesByMeetupId(UUID meetupId, int size, Long cursorId);
}
