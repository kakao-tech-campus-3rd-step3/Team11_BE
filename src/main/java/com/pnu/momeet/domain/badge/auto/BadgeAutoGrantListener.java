package com.pnu.momeet.domain.badge.auto;

import com.pnu.momeet.common.logging.LogTags;
import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import com.pnu.momeet.domain.meetup.event.MeetupFinishedEvent;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BadgeAutoGrantListener {

    private final BadgeRuleEngine badgeRuleEngine;
    private final BadgeAwarder badgeAwarder;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEvaluationSubmitted(EvaluationSubmittedEvent e) {
        long startMillis = System.currentTimeMillis();
        log.info("{} eventId={} type={} meetupId={} evaluatorProfileId={} targetProfileId={} rating={}",
            LogTags.HANDLE_START, e.eventId(), e.type(), e.meetupId(), e.evaluatorProfileId(), e.targetProfileId(), e.rating());

        List<String> codes = badgeRuleEngine.evaluateOnEvaluationSubmitted(e);
        codes.forEach(code -> badgeAwarder.award(e.targetProfileId(), code));

        long elapsedMs = System.currentTimeMillis() - startMillis;
        log.info("{} eventId={} type={} awardedCount={} elapsedMs={}",
            LogTags.HANDLE_END, e.eventId(), e.type(), codes.size(), elapsedMs);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMeetupFinished(MeetupFinishedEvent e) {
        long startMillis = System.currentTimeMillis();
        int participants = e.participantProfileIds() == null ? 0 : e.participantProfileIds().size();
        log.info("{} eventId={} type={} meetupId={} participants={}",
            LogTags.HANDLE_START, e.eventId(), e.type(), e.meetupId(), participants);

        int awarded = 0;
        if (e.participantProfileIds() != null) {
            for (UUID pid : e.participantProfileIds()) {
                List<String> codes = badgeRuleEngine.evaluateOnMeetupFinished(pid);
                codes.forEach(code -> badgeAwarder.award(pid, code));
                awarded += codes.size();
            }
        }

        long elapsedMs = System.currentTimeMillis() - startMillis;
        log.info("{} eventId={} type={} awardedCount={} participants={} elapsedMs={}",
            LogTags.HANDLE_END, e.eventId(), e.type(), awarded, participants, elapsedMs);
    }
}
