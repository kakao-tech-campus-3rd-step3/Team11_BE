package com.pnu.momeet.unit.chatting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pnu.momeet.domain.chatting.dto.response.MessageResponse;
import com.pnu.momeet.domain.chatting.entity.ChatMessage;
import com.pnu.momeet.domain.chatting.service.ChatMessageEntityService;
import com.pnu.momeet.domain.chatting.service.ChatMessageService;
import com.pnu.momeet.domain.common.dto.response.CursorInfo;
import com.pnu.momeet.domain.participant.service.ParticipantEntityService;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Mock
    private ParticipantEntityService participantService;

    @Mock
    private ChatMessageEntityService entityService;

    @Mock
    private ProfileEntityService profileService;

    @Test
    @DisplayName("성공: 참가자일 때 EntityService 위임 + CursorInfo 변환(nextId/hasNext 보존)")
    void getHistories_success_delegate_and_convert() {
        // given
        UUID meetupId = UUID.randomUUID();
        UUID viewerMemberId = UUID.randomUUID();
        UUID viewerProfileId = UUID.randomUUID();
        int size = 20;
        Long cursorId = 123L;

        Profile viewerProfile = mock(Profile.class);
        when(viewerProfile.getId()).thenReturn(viewerProfileId);
        when(profileService.getByMemberId(viewerMemberId)).thenReturn(viewerProfile);

        when(participantService.existsByProfileIdAndMeetupId(viewerProfileId, meetupId))
            .thenReturn(true);

        CursorInfo<ChatMessage> repoResult = new CursorInfo<>(List.of(), 999L);
        when(entityService.getHistories(meetupId, viewerMemberId, size, cursorId))
            .thenReturn(repoResult);

        // when
        CursorInfo<MessageResponse> result =
            chatMessageService.getHistories(meetupId, viewerMemberId, size, cursorId);

        // then
        verify(profileService).getByMemberId(viewerMemberId);
        verify(participantService).existsByProfileIdAndMeetupId(viewerProfileId, meetupId);
        verify(entityService).getHistories(meetupId, viewerMemberId, size, cursorId);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getSize()).isZero();
        assertThat(result.getNextId()).isEqualTo("999");
        assertThat(result.isHasNext()).isTrue();
    }

    @Test
    @DisplayName("성공: cursorId=null일 때도 위임 정확, hasNext=false")
    void getHistories_nullCursor_delegate_ok() {
        // given
        UUID meetupId = UUID.randomUUID();
        UUID viewerMemberId = UUID.randomUUID();
        UUID viewerProfileId = UUID.randomUUID();
        int size = 50;

        Profile viewerProfile = mock(Profile.class);
        when(viewerProfile.getId()).thenReturn(viewerProfileId);
        when(profileService.getByMemberId(viewerMemberId)).thenReturn(viewerProfile);

        when(participantService.existsByProfileIdAndMeetupId(viewerProfileId, meetupId))
            .thenReturn(true);

        // nextId 없음 -> hasNext=false, nextId=null 이어야 함
        CursorInfo<ChatMessage> repoResult = new CursorInfo<>(List.of());
        when(entityService.getHistories(meetupId, viewerMemberId, size, null))
            .thenReturn(repoResult);

        // when
        CursorInfo<MessageResponse> result =
            chatMessageService.getHistories(meetupId, viewerMemberId, size, null);

        // then
        verify(entityService).getHistories(meetupId, viewerMemberId, size, null);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getSize()).isZero();
        assertThat(result.getNextId()).isNull();
        assertThat(result.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("실패: 모임 참가자가 아니면 NoSuchElementException")
    void getHistories_notParticipant_throws() {
        // given
        UUID meetupId = UUID.randomUUID();
        UUID viewerMemberId = UUID.randomUUID();
        UUID viewerProfileId = UUID.randomUUID();

        Profile viewerProfile = mock(Profile.class);
        when(viewerProfile.getId()).thenReturn(viewerProfileId);
        when(profileService.getByMemberId(viewerMemberId)).thenReturn(viewerProfile);

        when(participantService.existsByProfileIdAndMeetupId(viewerProfileId, meetupId))
            .thenReturn(false);

        // when & then
        assertThatThrownBy(() ->
            chatMessageService.getHistories(meetupId, viewerMemberId, 10, null)
        ).isInstanceOf(NoSuchElementException.class);

        verify(entityService, never()).getHistories(any(), any(), anyInt(), any());
    }
}