package com.pnu.momeet.domain.member.service.scheduler;

import com.pnu.momeet.domain.member.service.MemberEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberScheduler {
    MemberEntityService memberService;

    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정마다 실행
    public void deleteUnverifiedMembers() {
        try {
            long deletedCount = memberService.deleteUnverifiedMembers();
            log.info("미 인증 회원 삭제 완료. 삭제된 회원 수: {}", deletedCount);
        } catch (Exception e) {
            log.error("미 인증 회원 삭제 중 오류 발생: {}", e.getMessage());
        }
    }
}
