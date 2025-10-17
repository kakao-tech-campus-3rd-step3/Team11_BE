package com.pnu.momeet.domain.chatting.service.listener;


import com.pnu.momeet.domain.chatting.service.ChatEventService;
import com.pnu.momeet.domain.chatting.service.ChatMessageEntityService;
import com.pnu.momeet.domain.evaluation.event.EvaluationDeadlineEndedEvent;
import com.pnu.momeet.domain.meetup.event.MeetupCanceledEvent;
import com.pnu.momeet.domain.meetup.event.MeetupFinishedEvent;
import com.pnu.momeet.domain.meetup.event.MeetupStartEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChattingActionEventListener {
    private final ChatEventService chatEventService;
    private final ChatMessageEntityService entityService;

    @Async
    @TransactionalEventListener(phase= TransactionPhase.AFTER_COMMIT)
    public void handleOnMeetupStarted(MeetupStartEvent event) {
        chatEventService.startMeetup(event.getMeetupId());
    }

    @Async
    @TransactionalEventListener(phase= TransactionPhase.AFTER_COMMIT)
    public void handleOnMeetupCanceled(MeetupCanceledEvent event) {
        switch (event.getRequestedBy()) {
            case ROLE_USER -> chatEventService.cancelMeetup(event.getMeetupId());
            case ROLE_ADMIN -> chatEventService.cancelByAdminMeetup(event.getMeetupId());
        }
    }

    @Async
    @TransactionalEventListener(phase= TransactionPhase.AFTER_COMMIT)
    public void handleOnMeetupFinished(MeetupFinishedEvent event) {
        chatEventService.finishMeetup(event.getMeetupId());
    }

    @Order(1)
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleEvaluationDeadlineEnded(EvaluationDeadlineEndedEvent event) {
        UUID meetupId = event.getMeetupId();
        log.debug("모임 평가 마감 - 채팅 메시지 삭제 처리 시작, meetupId: {}", meetupId);
        entityService.deleteAllByMeetupId(meetupId);
        log.debug("모임 평가 마감 - 채팅 메시지 삭제 처리 완료, meetupId: {}", meetupId);
    }
}
