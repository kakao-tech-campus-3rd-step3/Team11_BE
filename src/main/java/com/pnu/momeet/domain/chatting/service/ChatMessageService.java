package com.pnu.momeet.domain.chatting.service;

import com.pnu.momeet.domain.chatting.dto.request.MessageRequest;
import com.pnu.momeet.domain.chatting.dto.response.MessageResponse;
import com.pnu.momeet.domain.chatting.entity.ChatMessage;
import com.pnu.momeet.domain.chatting.enums.ChatMessageType;
import com.pnu.momeet.domain.chatting.repository.ChatMessageDslRepository;
import com.pnu.momeet.domain.chatting.repository.ChatMessageRepository;
import com.pnu.momeet.domain.chatting.service.mapper.ChatEntityMapper;
import com.pnu.momeet.domain.common.dto.response.CursorInfo;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.service.MeetupService;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.service.ParticipantService;
import com.pnu.momeet.domain.profile.entity.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageDslRepository chatMessageDslRepository;
    private final ParticipantService participantService;
    private final MeetupService meetupService;

    @Transactional
    public MessageResponse saveMessage(UUID meetupId, UUID memberId, MessageRequest message) {
        Participant participant = participantService.getEntityByMemberIdAndMeetupId(memberId, meetupId);
        Meetup meetup = meetupService.getReferenceById(meetupId);
        Profile profile = participant.getProfile();
        ChatMessageType type = ChatMessageType.valueOf(message.type());

        ChatMessage chatMessage = new ChatMessage(meetup, participant, profile, type, message.content());
        participant.setLastActiveAt(LocalDateTime.now());

        return ChatEntityMapper.toMessage(chatMessageRepository.save(chatMessage));
    }

    @Transactional(readOnly = true)
    public CursorInfo<MessageResponse> getHistories(UUID meetupId, UUID memberId, int size, Long cursorId) {
        // 모임 및 참가자 존재 여부 확인
        participantService.getEntityByMemberIdAndMeetupId(memberId, meetupId);

        CursorInfo<ChatMessage> histories =
                chatMessageDslRepository.findHistoriesByMeetupId(meetupId, size, cursorId);

        return CursorInfo.convert(histories, ChatEntityMapper::toMessage);
    }
}
