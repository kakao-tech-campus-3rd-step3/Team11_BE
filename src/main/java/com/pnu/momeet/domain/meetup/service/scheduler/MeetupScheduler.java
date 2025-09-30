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
    private static final int CHECK_INTERVAL_SEC = 60; // 1 분
    private static final int IDLE_TIMEOUT_SEC = 30; // 30 초
    private final MeetupStateService meetupStateService;
    private final MeetupEntityService meetupEntityService;

    @Scheduled(fixedDelay = CHECK_INTERVAL_SEC * 1000)
    public void checkAndFinishMeetups() {
        log.debug("종료할 모임 탐색 시작");

        // 1분 ~ 1분 30초 전 사이에 종료되는 모임 조회
        LocalDateTime limit = LocalDateTime.now().plusSeconds(IDLE_TIMEOUT_SEC);
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
}
