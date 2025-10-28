package com.pnu.momeet.domain.chatting.service.listener;


import com.pnu.momeet.domain.chatting.service.ChatMessageEntityService;
import com.pnu.momeet.domain.evaluation.event.EvaluationDeadlineEndedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageEventListener {
    private final ChatMessageEntityService entityService;

    @Order(1)
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleEvaluationDeadlineEnded(EvaluationDeadlineEndedEvent event) {
        UUID meetupId = event.getMeetupId();
        log.debug("모임 평가 마감 - 채팅 메시지 삭제 처리 시작, meetupId: {}", meetupId);
        entityService.deleteAllByMeetupId(meetupId);
        log.debug("모임 평가 마감 - 채팅 메시지 삭제 처리 완료, meetupId: {}", meetupId);
    }
}
