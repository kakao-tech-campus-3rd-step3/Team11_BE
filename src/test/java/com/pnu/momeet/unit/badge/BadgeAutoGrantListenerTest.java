package com.pnu.momeet.unit.badge;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.domain.badge.auto.BadgeAutoGrantListener;
import com.pnu.momeet.domain.badge.service.BadgeAwardService;
import com.pnu.momeet.domain.badge.enums.BadgeRule;
import com.pnu.momeet.domain.badge.service.BadgeRuleService;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import com.pnu.momeet.domain.meetup.event.MeetupFinishedEvent;
import com.pnu.momeet.domain.member.enums.Role;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.pnu.momeet.domain.member.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class BadgeAutoGrantListenerTest {

    @Mock
    BadgeRuleService badgeRuleService;

    @Mock
    BadgeAwardService badgeAwardService;

    @InjectMocks
    BadgeAutoGrantListener listener;

    @Test
    @DisplayName("모임 종료 이벤트 - 모든 참가자에 대해 배지 코드 부여")
    void handleMeetupFinishedEvent_awardsAllParticipants() {
        UUID meetupId = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        MeetupFinishedEvent e = new MeetupFinishedEvent(
            meetupId,
            List.of(p1, p2),
            Role.ROLE_USER
        );

        // 문자열 리터럴 대신 BadgeRule enum의 code() 사용
        given(badgeRuleService.evaluateOnMeetupFinished(p1))
            .willReturn(List.of(
                BadgeRule.FIRST_JOIN.getCode(),
                BadgeRule.TEN_JOINS.getCode()
            ));
        given(badgeRuleService.evaluateOnMeetupFinished(p2))
            .willReturn(List.of());

        listener.onMeetupFinished(e);

        // p1은 2개 코드 부여
        verify(badgeAwardService).award(p1, BadgeRule.FIRST_JOIN.getCode());
        verify(badgeAwardService).award(p1, BadgeRule.TEN_JOINS.getCode());
        // p2는 없음
        verify(badgeAwardService, never()).award(eq(p2), anyString());
    }

    @Test
    @DisplayName("평가 제출 이벤트 - 대상자에게 배지 코드 부여")
    void handleEvaluationSubmitted_awardsTarget() {
        UUID meetupId = UUID.randomUUID();
        UUID evaluator = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        EvaluationSubmittedEvent e = new EvaluationSubmittedEvent(
            meetupId,
            evaluator,
            target,
            Rating.LIKE
        );

        // 문자열 리터럴 대신 BadgeRule enum의 code() 사용
        given(badgeRuleService.evaluateOnEvaluationSubmitted(e))
            .willReturn(List.of(BadgeRule.LIKE_10.getCode()));

        listener.onEvaluationSubmitted(e);

        verify(badgeAwardService).award(target, BadgeRule.LIKE_10.getCode());
    }
}
