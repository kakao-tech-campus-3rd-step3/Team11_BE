package com.pnu.momeet.domain.chatting.repository;

import com.pnu.momeet.domain.chatting.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

}
