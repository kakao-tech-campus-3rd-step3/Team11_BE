package com.pnu.momeet.domain.meetup.service.listener;

import com.pnu.momeet.domain.chatting.enums.ChatActionType;
import com.pnu.momeet.domain.chatting.util.ChatMessagingTemplate;
import com.pnu.momeet.domain.meetup.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetupActionHandlingListener {
    private final ChatMessagingTemplate messagingTemplate;

    @Async
    @TransactionalEventListener(phase= TransactionPhase.AFTER_COMMIT)
    public void handleMeetupModified(MeetupModifiedEvent event) {
        UUID meetupId = event.getMeetupId();
        messagingTemplate.sendAction(meetupId, ChatActionType.MODIFIED);
        log.info("모임 수정 알림 전송 완료 - meetupId: {}", meetupId);
    }

    @Async
    @EventListener
    public void handleOnMeetupNearlyStarted(MeetupNearStartEvent event) {
        UUID meetupId = event.getMeetupId();
        messagingTemplate.sendAction(meetupId, ChatActionType.NEAR_STARTED);
        log.info("모임 시작 임박 알림 전송 완료 - meetupId: {}", meetupId);
    }

    @Async
    @TransactionalEventListener(phase= TransactionPhase.AFTER_COMMIT)
    public void handleOnMeetupStarted(MeetupStartEvent event) {
        UUID meetupId = event.getMeetupId();
        messagingTemplate.sendAction(meetupId, ChatActionType.STARTED);
        log.info("모임 시작 알림 전송 완료 - meetupId: {}", meetupId);
    }

    @Async
    @TransactionalEventListener(phase= TransactionPhase.AFTER_COMMIT)
    public void handleOnMeetupCanceled(MeetupCanceledEvent event) {
        UUID meetupId = event.getMeetupId();
        messagingTemplate.sendAction(meetupId, ChatActionType.CANCELED);
        log.info("모임 취소 알림 전송 완료 - meetupId: {}", meetupId);
    }

    @Async
    @EventListener
    public void handleOnMeetupNearlyEnded(MeetupNearEndEvent event) {
        UUID meetupId = event.getMeetupId();
        messagingTemplate.sendAction(meetupId, ChatActionType.NEAR_END);
        log.info("모임 종료 임박 알림 전송 완료 - meetupId: {}", meetupId);
    }

    @Async
    @TransactionalEventListener(phase= TransactionPhase.AFTER_COMMIT)
    public void handleOnMeetupFinished(MeetupFinishedEvent event) {
        UUID meetupId = event.getMeetupId();
        messagingTemplate.sendAction(meetupId, ChatActionType.END);
        log.info("모임 종료 알림 전송 완료 - meetupId: {}", meetupId);
    }
}
