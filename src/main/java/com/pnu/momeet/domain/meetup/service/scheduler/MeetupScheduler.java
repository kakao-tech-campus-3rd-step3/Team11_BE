package com.pnu.momeet.domain.meetup.service.scheduler;

import com.pnu.momeet.common.event.CoreEventPublisher;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import com.pnu.momeet.domain.meetup.service.MeetupEntityService;
import com.pnu.momeet.domain.meetup.service.MeetupStateService;
import com.pnu.momeet.domain.meetup.service.mapper.MeetupEntityMapper;
import com.pnu.momeet.domain.member.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetupScheduler {
    private static final int IDLE_TIMEOUT_SEC = 10; // 10 초
    private static final int EVALUATION_TIMEOUT_DAY = 3; // 3 일
    private static final int MEETUP_INTERVAL_MIN = 10; // 10 분

    private final MeetupStateService meetupStateService;
    private final MeetupEntityService meetupEntityService;
    private final CoreEventPublisher coreEventPublisher;

    @Scheduled(cron = "30 0/10 * * * ?")
    public void transmitMeetupState() {
        log.debug("종료할 모임 탐색 시작");
        alertNearlyStartMeetup();
        startOpenMeetup();
        alertNearlyFinishMeetup();
        finishProgressingMeetup();
    }

    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정마다 실행
    public void cleanUpMeetups() {
        List<Meetup> meetupsToClose = meetupEntityService.getAllByStatusAndEndAtBefore(
                MeetupStatus.ENDED, LocalDateTime.now().minusDays(EVALUATION_TIMEOUT_DAY).plusSeconds(IDLE_TIMEOUT_SEC)
        );
        taskExecutor("평가 기간 종료" , meetupsToClose, meetupStateService::evaluationPeriodEnded);
    }

    private void taskExecutor(String taskName, List<Meetup> meetups, Consumer<Meetup> task) {
        log.debug("{}할 모임 탐색 완료. {}할 모임 개수: {}", taskName, taskName, meetups.size());
        int successCount = 0, failureCount = 0;

        for (var meetup : meetups) {
            try {
                task.accept(meetup);
                successCount++;
            } catch (Exception e) {
                log.error("{} 처리 중 오류 발생. meetupId={}", taskName, meetup.getId(), e);
                failureCount++;
            }
        }
        log.info("{} 처리 완료. 대상 모임 : {}, 성공: {}, 실패: {}", taskName, meetups.size(), successCount, failureCount);
    }

    private void alertNearlyStartMeetup() {
        var meetupsToAlert = meetupEntityService.getAllByStatusAndStartAtBefore(
                MeetupStatus.OPEN,
                LocalDateTime.now().plusMinutes(MEETUP_INTERVAL_MIN).plusSeconds(IDLE_TIMEOUT_SEC)
        );
        taskExecutor("시작 임박 알림", meetupsToAlert, meetup ->
            coreEventPublisher.publish(MeetupEntityMapper.toMeetupNearStartEvent(meetup))
        );
    }

    private void startOpenMeetup() {
        var meetupsToStart = meetupEntityService.getAllByStatusAndStartAtBefore(
                MeetupStatus.OPEN, LocalDateTime.now().plusSeconds(IDLE_TIMEOUT_SEC)
        );
        taskExecutor("모임 시작", meetupsToStart, meetup ->
            meetupStateService.startMeetupById(meetup.getId())
        );
    }

    private void alertNearlyFinishMeetup() {
        var meetupsToAlert = meetupEntityService.getAllByStatusAndEndAtBefore(
                MeetupStatus.IN_PROGRESS,
                LocalDateTime.now().plusMinutes(MEETUP_INTERVAL_MIN).plusSeconds(IDLE_TIMEOUT_SEC)
        );
        taskExecutor("종료 임박 알림", meetupsToAlert, meetup ->
            coreEventPublisher.publish(MeetupEntityMapper.toMeetupNearEndEvent(meetup))
        );
    }

    private void finishProgressingMeetup() {
        // 1분 ~ 1분 30초 전 사이에 종료되는 모임 조회
        var meetupsToFinish = meetupEntityService.getAllByStatusAndEndAtBefore(
                MeetupStatus.IN_PROGRESS, LocalDateTime.now().plusSeconds(IDLE_TIMEOUT_SEC)
        );
        taskExecutor("모임 종료", meetupsToFinish, meetup -> meetupStateService.finishMeetupById(meetup.getId(), Role.ROLE_SYSTEM));
    }
}
