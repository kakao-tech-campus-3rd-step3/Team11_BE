package com.pnu.momeet.domain.participant.service.listener;

import com.pnu.momeet.domain.evaluation.event.EvaluationDeadlineEndedEvent;
import com.pnu.momeet.domain.participant.service.ParticipantEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParticipantListener {
    private final ParticipantEntityService entityService;

    @Order(2)
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleEvaluationDeadlineEndedEvent(EvaluationDeadlineEndedEvent event) {
        log.debug("모임 평가 마감 이벤트 수신 - meetupId: {}", event.getMeetupId());
        entityService.deleteAllByMeetupId(event.getMeetupId());
        log.debug("모임 평가 마감 처리 완료 - meetupId: {}", event.getMeetupId());
    }
}
