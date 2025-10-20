package com.pnu.momeet.domain.chatting.repository;

import com.pnu.momeet.domain.chatting.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Modifying
    @Query("DELETE FROM ChatMessage cm WHERE cm.meetup.id = :meetupId")
    void deleteAllByMeetupId(UUID meetupId);
}
