package com.pnu.momeet.domain.chatting.repository;

import com.pnu.momeet.domain.chatting.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    void deleteAllByMeetupId(UUID meetupId);
}
