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
        startOpenMeetup();
        alertNearlyFinishMeetup();
        finishProgressingMeetup();
        cancelIdleMeetup();
    }

    private void startOpenMeetup() {
        LocalDateTime limit = LocalDateTime.now().plusSeconds(IDLE_TIMEOUT_SEC);
        var meetupsToStart = meetupEntityService.getAllByStatusAndStartAtBefore(
                MeetupStatus.OPEN, limit
        );
        log.debug("시작할 모임 탐색 완료. 시작할 모임 개수: {}", meetupsToStart.size());
        int successCount = 0, failureCount = 0;

        for (var meetup : meetupsToStart) {
            try {
                meetupStateService.startMeetupById(meetup.getId());
                successCount++;
            } catch (Exception e) {
                log.error("모임 시작 처리 중 오류 발생. meetupId={}", meetup.getId(), e);
                failureCount++;
            }
        }
        log.info("모임 시작 처리 완료. 대상 모임 : {}, 성공: {}, 실패: {}", meetupsToStart.size(), successCount, failureCount);
    }

    private void alertNearlyFinishMeetup() {
        LocalDateTime alertLimit = LocalDateTime.now().plusMinutes(MEETUP_INTERVAL_MIN).plusSeconds(IDLE_TIMEOUT_SEC);
        var meetupsToAlert = meetupEntityService.getAllByStatusAndEndAtBefore(MeetupStatus.IN_PROGRESS, alertLimit);
        log.debug("종료 임박 모임 탐색 완료. 종료 임박 모임 개수: {}", meetupsToAlert.size());
        int successCount = 0, failureCount = 0;

        for (var meetup : meetupsToAlert) {
            try {
                coreEventPublisher.publish(MeetupEntityMapper.toMeetupNearEndEvent(meetup));
                successCount++;
            } catch (Exception e) {
                log.error("종료 임박 알림 처리 중 오류 발생. meetupId={}", meetup.getId(), e);
                failureCount++;
            }
        }
        log.info("종료 임박 알림 처리 완료. 대상 모임 : {}, 성공: {}, 실패: {}", meetupsToAlert.size(), successCount, failureCount);
    }

    private void finishProgressingMeetup() {
        // 1분 ~ 1분 30초 전 사이에 종료되는 모임 조회
        LocalDateTime limit = LocalDateTime.now().plusSeconds(IDLE_TIMEOUT_SEC);
        var meetupsToFinish = meetupEntityService.getAllByStatusAndEndAtBefore(
                MeetupStatus.IN_PROGRESS, limit
        );
        log.debug("종료할 모임 탐색 완료. 종료할 모임 개수: {}", meetupsToFinish.size());
        int successCount = 0, failureCount = 0;

        for (var meetup : meetupsToFinish) {
            try {
                meetupStateService.finishMeetupById(meetup.getId(), Role.ROLE_SYSTEM);
                successCount++;
            } catch (Exception e) {
                log.error("모임 종료 처리 중 오류 발생. meetupId={}", meetup.getId(), e);
                failureCount++;
            }
        }
        log.info("모임 종료 처리 완료. 대상 모임 : {}, 성공: {}, 실패: {}", meetupsToFinish.size(), successCount, failureCount);
    }

    private void cancelIdleMeetup() {
        LocalDateTime limit = LocalDateTime.now().minusSeconds(IDLE_TIMEOUT_SEC);
        var meetupsToCancel = meetupEntityService.getAllByStatusAndEndAtBefore(
                MeetupStatus.OPEN, limit
        );
        log.debug("취소할 모임 탐색 완료. 취소할 모임 개수: {}", meetupsToCancel.size());
        int successCount = 0, failureCount = 0;

        for (var meetup : meetupsToCancel) {
            try {
                meetupStateService.cancelMeetupById(meetup.getId());
                successCount++;
            } catch (Exception e) {
                log.error("모임 취소 처리 중 오류 발생. meetupId={}", meetup.getId(), e);
                failureCount++;
            }
        }
        log.info("모임 취소 처리 완료. 대상 모임 : {}, 성공: {}, 실패: {}", meetupsToCancel.size(), successCount, failureCount);
    }

    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정마다 실행
    public void cleanUpMeetups() {
        log.debug("평가 기간 종료할 모임 탐색 시작");
        LocalDateTime limit = LocalDateTime.now().minusDays(EVALUATION_TIMEOUT_DAY);
        List<Meetup> meetupsToClose = meetupEntityService.getAllByStatusAndEndAtBefore(MeetupStatus.ENDED, limit);
        int successCount = 0, failureCount = 0;

        for (var meetup : meetupsToClose) {
            try {
                meetupStateService.evaluationPeriodEnded(meetup);
                successCount++;
            } catch (Exception e) {
                log.error("평가 기간 종료 처리 중 오류 발생. meetupId={}", meetup.getId(), e);
                failureCount++;
            }
        }
        log.info("평가 기간 종료 처리 완료. 대상 모임: {}, 성공: {}, 실패: {}", meetupsToClose.size(), successCount, failureCount);
    }
}
