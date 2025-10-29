package com.pnu.momeet.domain.meetup.service.listener;

import com.pnu.momeet.domain.chatting.enums.ChatActionType;
import com.pnu.momeet.domain.chatting.util.ChatMessagingTemplate;
import com.pnu.momeet.domain.meetup.event.*;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.service.ParticipantEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetupActionHandlingListener {
    private final ParticipantEntityService participantService;
    private final ChatMessagingTemplate messagingTemplate;

    @Async
    @TransactionalEventListener(phase= TransactionPhase.AFTER_COMMIT)
    public void handleMeetupModified(MeetupModifiedEvent event) {
        UUID meetupId = event.getMeetupId();
        participantService.getAllByMeetupId(meetupId).stream()
                .filter(Participant::getIsActive)
                .forEach(participant ->
                        messagingTemplate.sendAction(meetupId, participant.getId(), ChatActionType.MODIFIED)
                );
        log.info("모임 수정 알림 전송 완료 - meetupId: {}", meetupId);
    }

    @Async
    @TransactionalEventListener(phase= TransactionPhase.AFTER_COMMIT)
    public void handleOnMeetupStarted(MeetupStartEvent event) {
        UUID meetupId = event.getMeetupId();
        participantService.getAllByMeetupId(meetupId).stream()
                .filter(Participant::getIsActive)
                .forEach(participant ->
                        messagingTemplate.sendAction(meetupId, participant.getId(), ChatActionType.STARTED)
                );
        log.info("모임 시작 알림 전송 완료 - meetupId: {}", meetupId);
    }

    @Async
    @TransactionalEventListener(phase= TransactionPhase.AFTER_COMMIT)
    public void handleOnMeetupCanceled(MeetupCanceledEvent event) {
        UUID meetupId = event.getMeetupId();
        participantService.getAllByMeetupId(meetupId).stream()
                .filter(Participant::getIsActive)
                .forEach(participant -> {
                    switch (event.getRequestedBy()) {
                        case ROLE_USER -> messagingTemplate.sendAction(meetupId, participant.getId(), ChatActionType.CANCELED);
                        case ROLE_ADMIN -> messagingTemplate.sendAction(meetupId, participant.getId(), ChatActionType.CANCELED_BY_ADMIN);
                        default -> {}
                    }
                });
        log.info("모임 취소 알림 전송 완료 - meetupId: {}", meetupId);
    }

    @Async
    @TransactionalEventListener(phase= TransactionPhase.AFTER_COMMIT)
    public void handleOnMeetupNearlyEnded(MeetupNearEndEvent event) {
        UUID meetupId = event.getMeetupId();
        participantService.getAllByMeetupId(meetupId).stream()
                .filter(Participant::getIsActive)
                .forEach(participant ->
                        messagingTemplate.sendAction(meetupId, participant.getId(), ChatActionType.NEAR_END)
                );
        log.info("모임 종료 임박 알림 전송 완료 - meetupId: {}", meetupId);
    }

    @Async
    @TransactionalEventListener(phase= TransactionPhase.AFTER_COMMIT)
    public void handleOnMeetupFinished(MeetupFinishedEvent event) {
        UUID meetupId = event.getMeetupId();
        participantService.getAllByMeetupId(meetupId).stream()
                .filter(Participant::getIsActive)
                .forEach(participant -> {
                    switch (event.getRequestedBy()) {
                        case ROLE_USER -> messagingTemplate.sendAction(meetupId, participant.getId(), ChatActionType.END);
                        case ROLE_ADMIN -> messagingTemplate.sendAction(meetupId, participant.getId(), ChatActionType.END_BY_ADMIN);
                        case ROLE_SYSTEM -> messagingTemplate.sendAction(meetupId, participant.getId(), ChatActionType.END_BY_SYSTEM);
                    }
                });
        log.info("모임 종료 알림 전송 완료 - meetupId: {}", meetupId);
    }
}
