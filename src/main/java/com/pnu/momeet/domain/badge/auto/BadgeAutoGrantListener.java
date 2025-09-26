package com.pnu.momeet.domain.badge.auto;

import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import com.pnu.momeet.domain.meetup.event.MeetupFinishedEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BadgeAutoGrantListener {

    private final BadgeRuleEngine badgeRuleEngine;
    private final BadgeAwarder badgeAwarder;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMeetupFinishedEvent(MeetupFinishedEvent e) {
        // 종료된 모임의 참가자 각각을 평가하고, 반환된 code 들을 부여
        for (UUID profileId : e.participantProfileIds()) {
            badgeRuleEngine.evaluateOnMeetupFinished(profileId)
                .forEach(code -> badgeAwarder.award(profileId, code));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEvaluationSubmitted(EvaluationSubmittedEvent e) {
        badgeRuleEngine.evaluateOnEvaluationSubmitted(e)
            .forEach(code -> badgeAwarder.award(e.targetProfileId(), code));
    }
}
