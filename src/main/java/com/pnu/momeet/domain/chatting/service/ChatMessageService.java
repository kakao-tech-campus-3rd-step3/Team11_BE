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
import com.pnu.momeet.domain.meetup.service.MeetupEntityService;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.service.ParticipantEntityService;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageDslRepository chatMessageDslRepository;
    private final ParticipantEntityService participantService;
    private final MeetupEntityService meetupService;
    private final ProfileEntityService profileService;

    @Transactional
    public MessageResponse saveMessage(UUID meetupId, UUID memberId, MessageRequest message) {
        Profile profile = profileService.getByMemberId(memberId);
        Meetup meetup = meetupService.getReferenceById(meetupId);
        Participant participant = participantService.getByProfileIDAndMeetupID(profile.getId(), meetupId);

        ChatMessageType type = ChatMessageType.valueOf(message.type());
        ChatMessage chatMessage = new ChatMessage(meetup, participant, profile, type, message.content());
        participant.setLastActiveAt(LocalDateTime.now());

        return ChatEntityMapper.toMessage(chatMessageRepository.save(chatMessage));
    }

    @Transactional(readOnly = true)
    public CursorInfo<MessageResponse> getHistories(UUID meetupId, UUID memberId, int size, Long cursorId) {
        // 모임 및 참가자 존재 여부 확인
        UUID profileId = profileService.getByMemberId(memberId).getId();
        if (!participantService.existsByProfileIdAndMeetupId(profileId, meetupId)) {
            throw new NoSuchElementException("해당 모임의 참가자가 아닙니다.");
        }
        CursorInfo<ChatMessage> histories =
                chatMessageDslRepository.findHistoriesByMeetupId(meetupId, size, cursorId);
        return CursorInfo.convert(histories, ChatEntityMapper::toMessage);
    }
}
