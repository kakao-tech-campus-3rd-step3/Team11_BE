package com.pnu.momeet.domain.profile.service.listener;

import com.pnu.momeet.common.config.TemperatureProperties;
import com.pnu.momeet.common.logging.LogTags;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
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
public class ProfileTemperatureAggregationListener {

    private final ProfileEntityService profileEntityService;
    private final TemperatureProperties temperatureProperties;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEvaluationSubmitted(EvaluationSubmittedEvent e) {
        final long started = System.currentTimeMillis();

        log.info("{} eventId={} type={} meetupId={} evaluatorProfileId={} targetProfileId={} rating={}",
            LogTags.HANDLE_START,
            e.getEventId(),
            e.type(),
            e.getMeetupId(),
            e.getEvaluatorProfileId(),
            e.getTargetProfileId(),
            e.getRating()
        );

        final UUID targetPid = e.getTargetProfileId();
        final Rating rating = e.getRating();

        // 동시성 제어 - 타깃 프로필 비관적 락
        Profile target = profileEntityService.getByIdForUpdate(targetPid);

        double k = temperatureProperties.priorK();

        switch (rating) {
            case LIKE    -> target.increaseLikesAndRecalc(k);
            case DISLIKE -> target.increaseDislikesAndRecalc(k);
            default -> {
                log.info("잘못된 평가입니다. eventId={}, rating={}", e.getEventId(), rating);
                return;
            }
        }

        long elapsed = System.currentTimeMillis() - started;
        log.info("{} eventId={} type={} pid={} likes={} dislikes={} temperature={} elapsedMs={}",
            LogTags.HANDLE_END,
            e.getEventId(),
            e.type(),
            target.getId(),
            target.getLikes(),
            target.getDislikes(),
            target.getTemperature(),
            elapsed
        );
    }
}