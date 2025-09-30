package com.pnu.momeet.domain.badge.auto;

import com.pnu.momeet.common.logging.LogTags;
import com.pnu.momeet.domain.badge.service.BadgeAwardService;
import com.pnu.momeet.domain.badge.service.BadgeRuleService;
import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import com.pnu.momeet.domain.meetup.event.MeetupFinishedEvent;
import java.util.List;
import java.util.UUID;
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

    private final BadgeRuleService badgeRuleService;
    private final BadgeAwardService badgeAwardService;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEvaluationSubmitted(EvaluationSubmittedEvent e) {
        long startMillis = System.currentTimeMillis();
        log.info("{} eventId={} type={} meetupId={} evaluatorProfileId={} targetProfileId={} rating={}",
            LogTags.HANDLE_START,
            e.getEventId(),
            e.type(),
            e.getMeetupId(),
            e.getEvaluatorProfileId(),
            e.getTargetProfileId(),
            e.getRating()
        );

        List<String> codes = badgeRuleService.evaluateOnEvaluationSubmitted(e);
        codes.forEach(code -> badgeAwardService.award(e.getTargetProfileId(), code));

        long elapsedMs = System.currentTimeMillis() - startMillis;
        log.info("{} eventId={} type={} awardedCount={} elapsedMs={}",
            LogTags.HANDLE_END, e.getEventId(), e.type(), codes.size(), elapsedMs);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMeetupFinished(MeetupFinishedEvent e) {
        long startMillis = System.currentTimeMillis();
        int participants = e.getParticipantProfileIds() == null ? 0 : e.getParticipantProfileIds().size();
        log.info("{} eventId={} type={} meetupId={} participants={}",
            LogTags.HANDLE_START, e.getEventId(), e.type(), e.getMeetupId(), participants);

        int awarded = 0;
        if (e.getParticipantProfileIds() != null) {
            for (UUID pid : e.getParticipantProfileIds()) {
                List<String> codes = badgeRuleService.evaluateOnMeetupFinished(pid);
                codes.forEach(code -> badgeAwardService.award(pid, code));
                awarded += codes.size();
            }
        }

        long elapsedMs = System.currentTimeMillis() - startMillis;
        log.info("{} eventId={} type={} awardedCount={} participants={} elapsedMs={}",
            LogTags.HANDLE_END, e.getEventId(), e.type(), awarded, participants, elapsedMs);
    }
}
