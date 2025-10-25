package com.pnu.momeet.domain.meetup.service.scheduler;

import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import com.pnu.momeet.domain.meetup.service.MeetupEntityService;
import com.pnu.momeet.domain.meetup.service.MeetupStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetupScheduler {
    private static final int IDLE_TIMEOUT_SEC = 30; // 30 초
    private static final int EVALUATION_TIMEOUT_DAY = 3; // 3 일

    private final MeetupStateService meetupStateService;
    private final MeetupEntityService meetupEntityService;

    @Scheduled(cron = "0 0/30 * * * ?")
    public void finishExpiredMeetups() {
        log.debug("종료할 모임 탐색 시작");
        // 30분 + 30초 전, 00분 + 30초 전 사이에 시작/종료되는 모임 처리
        LocalDateTime limit = LocalDateTime.now().plusSeconds(IDLE_TIMEOUT_SEC);
        startOpenMeetup(limit);
        finishProgressingMeetup(limit);
        cancelIdleMeetup(limit);
    }

    private void startOpenMeetup(LocalDateTime limit) {
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

    private void finishProgressingMeetup(LocalDateTime limit) {
        // 1분 ~ 1분 30초 전 사이에 종료되는 모임 조회
        var meetupsToFinish = meetupEntityService.getAllByStatusAndEndAtBefore(
                MeetupStatus.IN_PROGRESS, limit
        );
        log.debug("종료할 모임 탐색 완료. 종료할 모임 개수: {}", meetupsToFinish.size());
        int successCount = 0, failureCount = 0;

        for (var meetup : meetupsToFinish) {
            try {
                meetupStateService.finishMeetupById(meetup.getId());
                successCount++;
            } catch (Exception e) {
                log.error("모임 종료 처리 중 오류 발생. meetupId={}", meetup.getId(), e);
                failureCount++;
            }
        }
        log.info("모임 종료 처리 완료. 대상 모임 : {}, 성공: {}, 실패: {}", meetupsToFinish.size(), successCount, failureCount);
    }

    private void cancelIdleMeetup(LocalDateTime limit) {
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
    public void closeEvaluationPeriod() {
        log.debug("평가 기간 종료할 모임 탐색 시작");
        LocalDateTime limit = LocalDateTime.now().minusDays(EVALUATION_TIMEOUT_DAY);
        var meetupsToClose = meetupEntityService.getAllByStatusAndEndAtBefore(
                MeetupStatus.ENDED, limit
        );
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
